# bccsp_pointer.py
import math, time, json, random, argparse
from pathlib import Path

import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader

# ----------------------------
# 0) Repro + device
# ----------------------------
def set_seed(s=42):
    random.seed(s)
    torch.manual_seed(s)
    torch.cuda.manual_seed_all(s)

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

# ----------------------------
# 1) Utilities
# ----------------------------
def masked_softmax(logits, mask, dim=-1):
    # mask: 1 for feasible, 0 for infeasible
    very_neg = torch.finfo(logits.dtype).min
    logits = logits.masked_fill(mask == 0, very_neg)
    return F.softmax(logits, dim=dim)

def pairwise_dist(coords):
    # coords: [B,N,2]
    diff = coords.unsqueeze(2) - coords.unsqueeze(1)
    return diff.pow(2).sum(-1).sqrt()

# ----------------------------
# 2) BC-CSP Environment
# ----------------------------
class BCCSPEnv:
    """
    Budget-Constrained Covering Salesman Environment (batched).
    Visit nodes to cover demand; must return to depot within budget.
    """
    def __init__(self, coords, packets, cover, budget, depot=0):
        """
        coords:  [B,N,2] normalized to ~[0,1]
        packets: [B,N]  >=0 (value per node)
        cover:   [B,N,N] binary (i covers j), or None -> identity (visit==collect)
        budget:  [B]    max tour length (same metric as coords distance)
        """
        self.B, self.N, _ = coords.shape
        self.coords = coords
        self.dist = pairwise_dist(coords)  # [B,N,N]
        self.packets = packets
        if cover is None:
            eye = torch.eye(self.N, device=coords.device).unsqueeze(0).expand(self.B, -1, -1)
            cover = eye
        self.cover = cover
        self.depot = depot
        self.reset(budget)

    def reset(self, budget):
        self.budget0 = budget.clone()
        self.remaining = budget.clone()
        self.visited = torch.zeros(self.B, self.N, dtype=torch.bool, device=self.coords.device)
        self.covered = torch.zeros(self.B, self.N, dtype=torch.bool, device=self.coords.device)
        self.curr = torch.full((self.B,), self.depot, dtype=torch.long, device=self.coords.device)
        self.tour_len = torch.zeros(self.B, device=self.coords.device)
        self.prev_move = torch.zeros(self.B, device=self.coords.device)
        self.visited[:, self.depot] = True
        self.update_coverage(self.depot)
        return self.state()

    def update_coverage(self, node_idx):
        row = self.cover[torch.arange(self.B, device=self.coords.device), node_idx]  # [B,N]
        self.covered |= (row > 0)

    def feasible_mask(self):
        """
        Candidate i feasible if:
          - not visited
          - dist(curr,i) + dist(i,depot) <= remaining
        Always allow depot if return is feasible (STOP).
        """
        b = torch.arange(self.B, device=self.coords.device)
        d_to_i = self.dist[b, self.curr]          # [B,N]
        d_ret  = self.dist[:, :, self.depot]      # [B,N]
        need = d_to_i + d_ret
        feas = (need <= self.remaining.unsqueeze(1)) & (~self.visited)
        feas[:, self.depot] = (self.dist[b, self.curr, self.depot] <= self.remaining)
        return feas

    def marginal_gain(self):
        """Packets newly covered if we move to i (before stepping)."""
        not_cov = (~self.covered).float()         # [B,N]
        gains = torch.einsum('bij,bj->bi', self.cover.float(), not_cov * self.packets)
        gains[:, self.depot] = 0.0
        return gains

    def step(self, action):
        b = torch.arange(self.B, device=self.coords.device)
        d_move = self.dist[b, self.curr, action]
        self.remaining -= d_move
        self.tour_len += d_move
        self.prev_move = d_move
        self.curr = action
        self.visited[b, action] = True
        self.update_coverage(action)
        done = (action == self.depot) | (self.remaining < 1e-8)
        return self.state(), done

    def state(self):
        return {
            "visited": self.visited,
            "covered": self.covered,
            "remaining_frac": self.remaining / (self.budget0 + 1e-9),
            "curr": self.curr,
            "tour_len": self.tour_len,
            "prev_move": self.prev_move
        }

# ----------------------------
# 3) Model: Transformer encoder + Pointer decoder
# ----------------------------
class AttnEncoder(nn.Module):
    def __init__(self, node_dim, d_model=128, n_heads=8, n_layers=3, dropout=0.0):
        super().__init__()
        self.input = nn.Linear(node_dim, d_model)
        enc_layer = nn.TransformerEncoderLayer(
            d_model=d_model, nhead=n_heads, batch_first=True, dropout=dropout
        )
        self.enc = nn.TransformerEncoder(enc_layer, num_layers=n_layers)

    def forward(self, x):
        h = self.input(x)  # [B,N,d]
        return self.enc(h) # [B,N,d]

class PointerDecoder(nn.Module):
    def __init__(self, d_model=128, use_gain_bias=True, gain_bias_scale=0.2):
        super().__init__()
        # v^T tanh(W1*enc + W2*ctx)
        self.W1 = nn.Linear(d_model, d_model, bias=False)
        self.W2 = nn.Linear(d_model, d_model, bias=False)
        self.v  = nn.Linear(d_model, 1, bias=False)
        self.ctx = nn.Sequential(
            nn.Linear(d_model + 4, d_model), nn.ReLU(),
            nn.Linear(d_model, d_model)
        )
        self.use_gain_bias = use_gain_bias
        self.gain_bias_scale = gain_bias_scale

    def forward(self, enc, dyn_feat, feas_mask, pointer_idx, gain=None):
        """
        enc: [B,N,d]
        dyn_feat: [B,4]  [remaining_frac, prev_move, tour_frac, covered_frac]
        pointer_idx: [B] current node index
        gain: [B,N] optional marginal gains to bias logits
        """
        B, N, d = enc.shape
        curr_emb = enc[torch.arange(B, device=enc.device), pointer_idx]  # [B,d]
        ctx = self.ctx(torch.cat([curr_emb, dyn_feat], dim=-1))          # [B,d]

        q = self.W2(ctx).unsqueeze(1).expand(-1, N, -1)                  # [B,N,d]
        k = self.W1(enc)                                                 # [B,N,d]
        e = torch.tanh(k + q)
        logits = self.v(e).squeeze(-1)                                   # [B,N]

        if self.use_gain_bias and gain is not None:
            g = gain / (gain.max(dim=1, keepdim=True).values + 1e-6)
            logits = logits + self.gain_bias_scale * torch.log1p(g)

        probs = masked_softmax(logits, feas_mask, dim=-1)
        return logits, probs

class AttnRouter(nn.Module):
    def __init__(self, node_dim, d_model=128, n_heads=8, n_layers=3, dropout=0.0):
        super().__init__()
        self.enc = AttnEncoder(node_dim, d_model, n_heads, n_layers, dropout)
        self.decoder = PointerDecoder(d_model)
        self.critic = nn.Sequential(
            nn.Linear(d_model + 4, d_model), nn.ReLU(), nn.Linear(d_model, 1)
        )

    def forward_step(self, enc, env: BCCSPEnv):
        """
        One decision step: logits/probs/value from current env state.
        """
        B, N, d = enc.shape
        feas = env.feasible_mask()                    # [B,N]
        gains = env.marginal_gain()                   # [B,N]

        rem = env.remaining / (env.budget0 + 1e-9)
        covered_frac = (env.covered.float().sum(-1) / N)
        tour_frac = env.tour_len / (env.budget0 + 1e-9)
        dyn = torch.stack([rem, env.prev_move, tour_frac, covered_frac], dim=-1)  # [B,4]

        logits, probs = self.decoder(enc, dyn, feas, env.curr, gain=gains)

        ctx_vec = torch.cat([enc[torch.arange(B, device=enc.device), env.curr], dyn], dim=-1)
        value = self.critic(ctx_vec).squeeze(-1)      # [B]
        return logits, probs, value

    def rollout(self, node_feats, env: BCCSPEnv, decode='sample', max_steps=None):
        """
        Roll out a full episode (until STOP or budget).
        Returns: actions[T], logps[T], values[T]
        """
        enc = self.enc(node_feats)  # [B,N,d]
        B, N, _ = enc.shape
        actions, logps, values = [], [], []
        done = torch.zeros(B, dtype=torch.bool, device=node_feats.device)
        steps = 0
        max_steps = max_steps or (N + 5)

        while (~done).any() and steps < max_steps:
            logits, probs, value = self.forward_step(enc, env)
            values.append(value)

            if decode == 'greedy':
                a = probs.argmax(-1)
            else:
                a = torch.multinomial(probs, 1).squeeze(-1)
            lp = (probs + 1e-12).log()[torch.arange(B, device=probs.device), a]

            actions.append(a)
            logps.append(lp)

            _, d = env.step(a)
            done |= d
            steps += 1

        if len(actions) == 0:
            zero = torch.zeros(B, 1, device=node_feats.device, dtype=torch.long)
            return zero, torch.zeros_like(zero, dtype=torch.float), torch.zeros_like(zero, dtype=torch.float)

        actions = torch.stack(actions, 1)  # [B,T]
        logps   = torch.stack(logps, 1)    # [B,T]
        values  = torch.stack(values, 1)   # [B,T]
        return actions, logps, values

# ----------------------------
# 4) Synthetic data (for quick smoke tests)
# ----------------------------
def synth_batch(batch_size=64, N=30, radius=0.15, budget_scale=0.9, device=DEVICE, with_cover=True):
    coords = torch.rand(batch_size, N, 2, device=device)
    coords[:, 0] = 0.5  # depot near center
    packets = torch.randint(low=1, high=6, size=(batch_size, N), device=device).float()
    packets[:, 0] = 0.0

    if with_cover:
        r = torch.clamp(radius + 0.03*torch.randn(batch_size, N, device=device), 0.05, 0.4)
        dist = pairwise_dist(coords)
        cover = (dist <= r.unsqueeze(2)).float()
        cover[:, :, 0] = 0
    else:
        cover = None

    base = 0.7 * math.sqrt(N)
    budget = torch.full((batch_size,), budget_scale * base, device=device)

    if with_cover:
        feats = torch.cat([coords, packets.unsqueeze(-1), r.unsqueeze(-1)], dim=-1)
    else:
        feats = torch.cat([coords, packets.unsqueeze(-1), torch.zeros_like(packets.unsqueeze(-1))], dim=-1)
    return feats, packets, cover, budget

# ----------------------------
# 5) (Optional) Dataset for behavior cloning
# ----------------------------
class BCCSPDataset(Dataset):
    """
    JSONL file with per-line dict:
      nodes:  N x F list (first 2 must be coords)
      cover:  N x N 0/1 or null
      budget: float
      tour:   list of node indices (teacher), starts & ends with 0
      source: "ILP" | "PCA" (optional)
    """
    def __init__(self, jsonl_path):
        self.recs = []
        with open(jsonl_path, "r") as f:
            for ln in f:
                self.recs.append(json.loads(ln))

    def __len__(self):
        return len(self.recs)

    def __getitem__(self, idx):
        rec = self.recs[idx]
        nodes = torch.tensor(rec["nodes"], dtype=torch.float)
        cover = None if rec.get("cover") is None else torch.tensor(rec["cover"], dtype=torch.float)
        budget = torch.tensor(rec["budget"], dtype=torch.float)
        tour = torch.tensor(rec.get("tour", [0]), dtype=torch.long)
        source = rec.get("source", "PCA")
        return nodes, cover, budget, tour, source

def bc_collate(batch):
    nodes = torch.stack([b[0] for b in batch], 0).to(DEVICE)         # [B,N,F]
    covers = [b[1] for b in batch]
    cover = None if any(c is None for c in covers) else torch.stack(covers, 0).to(DEVICE)
    budget = torch.stack([b[2] for b in batch], 0).to(DEVICE)        # [B]
    tours = [b[3].to(DEVICE) for b in batch]
    T = min(t.shape[0] for t in tours)
    tour = torch.stack([t[:T] for t in tours], 0)                    # [B,T]
    src = [b[4] for b in batch]
    return nodes, cover, budget, tour, src

# ----------------------------
# 6) Training loops
# ----------------------------
def bc_epoch(model, loader, opt, ilp_weight=2.0, pca_weight=1.0):
    model.train()
    total_loss = 0.0
    ce = nn.CrossEntropyLoss(reduction="none")
    for nodes, cover, budget, tour, src in loader:
        B, N, F = nodes.shape
        coords = nodes[..., :2]
        packets = nodes[..., 2]
        env = BCCSPEnv(coords, packets, cover, budget)
        enc = model.enc(nodes)

        losses = []
        # teacher forcing along provided tour (use next node as label)
        for t in range(tour.size(1)-1):
            target = tour[:, t+1]  # [B]
            logits, probs, value = model.forward_step(enc, env)
            feas = env.feasible_mask().float()
            very_neg = torch.finfo(logits.dtype).min
            masked_logits = logits.masked_fill(feas == 0, very_neg)
            loss_t = ce(masked_logits, target)
            losses.append(loss_t)
            env.step(target)

        loss = torch.stack(losses, 0).mean(0)
        weights = torch.tensor([ilp_weight if s.upper()=="ILP" else pca_weight for s in src],
                               device=nodes.device, dtype=loss.dtype)
        loss = (loss * weights).mean()

        opt.zero_grad()
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        opt.step()
        total_loss += loss.item()
    return total_loss / max(1, len(loader))

def rl_epoch(model, batch_size=64, N=30, steps=1000, lr=1e-4, entropy_coef=0.01):
    model.train()
    opt = torch.optim.Adam(model.parameters(), lr=lr)
    running_R = 0.0
    for it in range(steps):
        nodes, packets, cover, budget = synth_batch(batch_size=batch_size, N=N, device=DEVICE)
        coords = nodes[..., :2]
        env = BCCSPEnv(coords, packets, cover, budget)
        actions, logps, values = model.rollout(nodes, env, decode='sample')

        total_packets = packets.sum(-1)
        covered_packets = (env.covered.float() * packets).sum(-1)
        tour_frac = env.tour_len / (env.budget0 + 1e-9)
        R = 2.0*(covered_packets / (total_packets + 1e-9)) - 1.0*tour_frac

        V = values.mean(1).squeeze(-1).detach()
        adv = (R - V)
        policy_loss = -(logps.sum(1) * adv).mean()
        value_loss  = F.mse_loss(values.mean(1).squeeze(-1), R)
        entropy = -(logps.exp() * logps).sum(1).mean() / max(1, logps.shape[1])
        loss = policy_loss + 0.5*value_loss - entropy_coef*entropy

        opt.zero_grad()
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        opt.step()

        running_R = 0.9*running_R + 0.1*R.mean().item()
        if (it+1) % 100 == 0:
            print(f"[RL] it {it+1:04d}  loss {loss.item():.3f}  "
                  f"R {R.mean().item():.3f}  "
                  f"covered {covered_packets.mean().item():.2f}/{total_packets.mean().item():.2f}  "
                  f"tour {env.tour_len.mean().item():.3f}")
    return running_R

# ----------------------------
# 7) JSONL RL support (for your real_networks.jsonl)
# ----------------------------
def load_jsonl_records(path):
    recs = []
    with open(path, "r") as f:
        for ln in f:
            recs.append(json.loads(ln))
    return recs

def sample_batch_from_jsonl(recs, batch_size, device):
    batch = random.sample(recs, min(batch_size, len(recs)))
    nodes = torch.tensor([b["nodes"] for b in batch], dtype=torch.float, device=device)  # [B,N,F]
    cover = None  # visit==collect for your dataset (converter set "cover": null)
    budget = torch.tensor([b["budget"] for b in batch], dtype=torch.float, device=device)  # [B]
    coords = nodes[..., :2]
    packets = nodes[..., 2]
    return nodes, coords, packets, cover, budget

def rl_epoch_jsonl(model, jsonl_path, steps=1000, batch_size=64, lr=1e-4, entropy_coef=0.01):
    model.train()
    opt = torch.optim.Adam(model.parameters(), lr=lr)
    recs = load_jsonl_records(jsonl_path)
    running_R = 0.0
    for it in range(steps):
        nodes, coords, packets, cover, budget = sample_batch_from_jsonl(recs, batch_size, DEVICE)
        env = BCCSPEnv(coords, packets, cover, budget)
        actions, logps, values = model.rollout(nodes, env, decode='sample')

        total_packets = packets.sum(-1)
        covered_packets = (env.covered.float() * packets).sum(-1)
        tour_frac = env.tour_len / (env.budget0 + 1e-9)
        R = 2.0*(covered_packets / (total_packets + 1e-9)) - 1.0*tour_frac  # 2:1 tradeoff

        V = values.mean(1).squeeze(-1).detach()
        adv = (R - V)
        policy_loss = -(logps.sum(1) * adv).mean()
        value_loss  = F.mse_loss(values.mean(1).squeeze(-1), R)
        entropy = -(logps.exp() * logps).sum(1).mean() / max(1, logps.shape[1])
        loss = policy_loss + 0.5*value_loss - entropy_coef*entropy

        opt.zero_grad()
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        opt.step()

        running_R = 0.9*running_R + 0.1*R.mean().item()
        if (it+1) % 100 == 0:
            print(f"[RL-jsonl] it {it+1:04d}  loss {loss.item():.3f}  "
                  f"R {R.mean().item():.3f}  "
                  f"covered {covered_packets.mean().item():.2f}/{total_packets.mean().item():.2f}  "
                  f"tour {env.tour_len.mean().item():.3f}")
    return running_R

# ----------------------------
# 8) Evaluation (synthetic quick check)
# ----------------------------
@torch.no_grad()
def eval_once(model, batch_size=128, N=30):
    model.eval()
    nodes, packets, cover, budget = synth_batch(batch_size=batch_size, N=N, device=DEVICE)
    coords = nodes[..., :2]
    env = BCCSPEnv(coords, packets, cover, budget)
    actions, _, _ = model.rollout(nodes, env, decode='greedy')
    total_packets = packets.sum(-1)
    covered_packets = (env.covered.float() * packets).sum(-1)
    tour_len = env.tour_len
    ret_rate = (env.curr == 0).float().mean().item()
    return {
        "packets_mean": covered_packets.mean().item(),
        "packets_pct": (covered_packets/ (total_packets+1e-9)).mean().item(),
        "tour_len": tour_len.mean().item(),
        "return_rate": ret_rate
    }

# ----------------------------
# 9) CLI
# ----------------------------
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mode", choices=["rl","bc"], default="rl",
                    help="Training mode: rl (policy gradient) or bc (imitation)")
    ap.add_argument("--jsonl", type=str, default="",
                    help="Path to JSONL for RL/BC (your real_networks.jsonl)")
    ap.add_argument("--epochs", type=int, default=3)
    ap.add_argument("--steps", type=int, default=1500, help="iterations per epoch (RL)")
    ap.add_argument("--batch_size", type=int, default=32)
    ap.add_argument("--N", type=int, default=30, help="node count for synthetic eval")
    ap.add_argument("--node_dim", type=int, default=4, help="features per node; using [x,y,packets,radius]")
    ap.add_argument("--d_model", type=int, default=128)
    ap.add_argument("--lr", type=float, default=1e-4)
    args = ap.parse_args()

    set_seed(42)
    model = AttnRouter(node_dim=args.node_dim, d_model=args.d_model).to(DEVICE)

    if args.mode == "bc":
        if not args.jsonl:
            raise SystemExit("Behavior cloning requires --jsonl with teacher tours.")
        ds = BCCSPDataset(args.jsonl)
        dl = DataLoader(ds, batch_size=args.batch_size, shuffle=True, collate_fn=bc_collate)
        opt = torch.optim.Adam(model.parameters(), lr=args.lr)
        for ep in range(1, args.epochs+1):
            loss = bc_epoch(model, dl, opt)
            metrics = eval_once(model, N=args.N)
            print(f"[BC] epoch {ep:02d}  loss {loss:.3f}  "
                  f"packets {metrics['packets_pct']*100:.1f}%  "
                  f"tour {metrics['tour_len']:.3f}  return {metrics['return_rate']*100:.1f}%")

    else:  # RL
        for ep in range(1, args.epochs+1):
            if args.jsonl and Path(args.jsonl).exists():
                avgR = rl_epoch_jsonl(model, jsonl_path=args.jsonl,
                                      steps=args.steps, batch_size=args.batch_size, lr=args.lr)
            else:
                avgR = rl_epoch(model, batch_size=args.batch_size, N=args.N,
                                steps=args.steps, lr=args.lr)
            metrics = eval_once(model, N=args.N)
            print(f"[RL] epoch {ep:02d}  R~ {avgR:.3f}  packets {metrics['packets_pct']*100:.1f}%  "
                  f"tour {metrics['tour_len']:.3f}  return {metrics['return_rate']*100:.1f}%")

if __name__ == "__main__":
    main()
