# networks_to_jsonl.py
import argparse, json, os, glob
import numpy as np

# Linear fit from your averages:
# 50Wh→1733.02, 70Wh→2389.4275, 90Wh→3004.5, 110Wh→3912.92
# dist_raw ≈ a*Wh + b
A, B = 35.7738625, -101.942125  # so dist ≈ 35.774*Wh - 101.942

BUDGETS_WH = [50, 70, 90, 110]

def read_network_txt(path):
    """
    Returns:
      ids:      [N] ints
      coords:   N×2 np.float64 (raw units)
      packets:  [N] np.float64
    Assumes columns: id x y packets
    """
    ids, xs, ys, pk = [], [], [], []
    with open(path, "r") as f:
        for ln in f:
            ln = ln.strip()
            if not ln: continue
            parts = ln.split()
            if len(parts) < 4: continue
            i, x, y, p = parts[:4]
            ids.append(int(i))
            xs.append(float(x))
            ys.append(float(y))
            pk.append(float(p))
    coords = np.stack([np.array(xs), np.array(ys)], axis=1)
    packets = np.array(pk, dtype=np.float64)
    return np.array(ids), coords, packets

def isotropic_normalize(coords):
    """
    Isotropic min-max normalization so Euclidean distances scale by 1/scale.
    coords_raw: N×2
    Returns:
      coords_norm: N×2 in [0,1] (ish)
      scale:       positive scalar (divide raw distances by 'scale' to get normalized)
    """
    mins = coords.min(axis=0)
    maxs = coords.max(axis=0)
    span = max((maxs - mins).max(), 1e-9)  # single scalar scale
    coords_norm = (coords - mins) / span
    return coords_norm, float(span)

def wh_to_distance_raw(wh):
    """Raw distance in the same units as your .txt coords, from Wh via linear fit."""
    return max(0.0, A * float(wh) + B)

def make_record(ids, coords_raw, packets, wh, depot_id=1):
    """
    Build one JSONL record for a given Wh budget.
    - Reorders so that depot is index 0
    - Normalizes coords isotropically
    - Converts Wh -> raw distance, then -> normalized budget
    - visit==collect → cover=None, radius=0
    - nodes: [x_norm, y_norm, packets, 0.0]
    """
    # Put depot first (id=1 by convention here)
    idx_depot = int(np.where(ids == depot_id)[0][0]) if depot_id in ids else 0
    order = [idx_depot] + [k for k in range(len(ids)) if k != idx_depot]
    coords_raw = coords_raw[order]
    packets = packets[order]

    # Normalize coords and budget
    coords_norm, scale = isotropic_normalize(coords_raw)
    budget_raw = wh_to_distance_raw(wh)            # raw distance
    budget_norm = budget_raw / (scale + 1e-9)      # match normalized coords

    # Build nodes [x,y,packets,radius]
    nodes = np.column_stack([
        coords_norm[:,0],
        coords_norm[:,1],
        packets,
        np.zeros_like(packets)  # radius=0 → visit==collect
    ]).tolist()

    rec = {
        "nodes": nodes,
        "cover": None,              # None => identity coverage in the model
        "budget": float(budget_norm),
        "tour": [0],                # no teacher route yet (RL-friendly)
        "source": "PCA"             # arbitrary; not used if no BC
    }
    return rec

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--in_dir", required=True, help="Folder with N1.txt ... N10.txt")
    ap.add_argument("--out", default="real_networks.jsonl")
    ap.add_argument("--depot_id", type=int, default=1, help="Which node id is the depot (default: 1)")
    args = ap.parse_args()

    paths = sorted(glob.glob(os.path.join(args.in_dir, "*.txt")))
    if not paths:
        raise SystemExit(f"No .txt files found in {args.in_dir}")

    out = open(args.out, "w")
    for p in paths:
        ids, coords, packets = read_network_txt(p)
        for wh in BUDGETS_WH:
            rec = make_record(ids, coords, packets, wh, depot_id=args.depot_id)
            out.write(json.dumps(rec) + "\n")
    out.close()
    print(f"Wrote {args.out} with {len(paths)*len(BUDGETS_WH)} records.")

if __name__ == "__main__":
    main()
