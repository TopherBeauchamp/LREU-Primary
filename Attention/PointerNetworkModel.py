import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader

# ----------------------------
# 1. Dataset
# ----------------------------
class NetworkDataset(Dataset):
    """
    Each sample: {'nodes': [[x, y, packets], ...], 'solution': [node_order]}
    """
    def __init__(self, data_list):
        self.data_list = data_list

    def __len__(self):
        return len(self.data_list)

    def __getitem__(self, idx):
        sample = self.data_list[idx]
        nodes = torch.tensor(sample['nodes'], dtype=torch.float)
        solution = torch.tensor(sample['solution'], dtype=torch.long)
        return nodes, solution

# ----------------------------
# 2. Pointer Network
# ----------------------------
class Encoder(nn.Module):
    def __init__(self, input_dim, hidden_dim):
        super().__init__()
        self.lstm = nn.LSTM(input_dim, hidden_dim, batch_first=True)

    def forward(self, x):
        outputs, (h, c) = self.lstm(x)
        return outputs, (h, c)

class Decoder(nn.Module):
    def __init__(self, hidden_dim):
        super().__init__()
        self.lstm = nn.LSTM(hidden_dim, hidden_dim, batch_first=True)
        self.pointer = nn.Linear(hidden_dim, hidden_dim)

    def forward(self, encoder_outputs, target_seq=None, teacher_forcing_ratio=0.5):
        batch_size, seq_len, hidden_dim = encoder_outputs.size()
        decoder_input = torch.zeros(batch_size, 1, hidden_dim).to(encoder_outputs.device)
        h, c = (torch.zeros(1, batch_size, hidden_dim).to(encoder_outputs.device),
                torch.zeros(1, batch_size, hidden_dim).to(encoder_outputs.device))
        outputs = []
        for t in range(seq_len):
            out, (h, c) = self.lstm(decoder_input, (h, c))
            # Compute attention
            attn_scores = torch.bmm(encoder_outputs, out.transpose(1, 2)).squeeze(2)
            attn_probs = F.softmax(attn_scores, dim=1)
            outputs.append(attn_probs)
            if target_seq is not None and torch.rand(1).item() < teacher_forcing_ratio:
                idx = target_seq[:, t].unsqueeze(1).unsqueeze(2).expand(-1, -1, hidden_dim)
                decoder_input = torch.gather(encoder_outputs, 1, idx)
            else:
                idx = attn_probs.argmax(dim=1).unsqueeze(1).unsqueeze(2).expand(-1, -1, hidden_dim)
                decoder_input = torch.gather(encoder_outputs, 1, idx)
        return torch.stack(outputs, dim=1)

class PointerNet(nn.Module):
    def __init__(self, input_dim, hidden_dim):
        super().__init__()
        self.encoder = Encoder(input_dim, hidden_dim)
        self.decoder = Decoder(hidden_dim)

    def forward(self, x, target_seq=None, teacher_forcing_ratio=0.5):
        enc_out, _ = self.encoder(x)
        return self.decoder(enc_out, target_seq, teacher_forcing_ratio)

# ----------------------------
# 3. Example Training Loop
# ----------------------------
def train_pointernet(model, dataloader, optimizer, device):
    model.train()
    criterion = nn.CrossEntropyLoss()
    for nodes, solution in dataloader:
        nodes, solution = nodes.to(device), solution.to(device)
        optimizer.zero_grad()
        outputs = model(nodes, solution)
        loss = 0
        seq_len = solution.size(1)
        for t in range(seq_len):
            loss += criterion(outputs[:, t, :], solution[:, t])
        loss.backward()
        optimizer.step()

# ----------------------------
# 4. Usage
# ----------------------------
# data_list = [{'nodes': [[x0,y0,p0], ...], 'solution': [0,3,5,...]}, ...]
# dataset = NetworkDataset(data_list)
# dataloader = DataLoader(dataset, batch_size=32, shuffle=True)
# device = 'cuda' if torch.cuda.is_available() else 'cpu'
# model = PointerNet(input_dim=3, hidden_dim=128).to(device)
# optimizer = torch.optim.Adam(model.parameters(), lr=1e-3)
# train_pointernet(model, dataloader, optimizer, device)
