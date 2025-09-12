from ortools.linear_solver import pywraplp

def solve_robot_network_ilp(Cost, p, battery_capacity):
    n = len(Cost)
    # Scale distances to battery units like Java (/36)
    scaled_Cost = [[Cost[i][j] / 36 for j in range(n)] for i in range(n)]
    scaled_battery = battery_capacity

    solver = pywraplp.Solver.CreateSolver('SCIP')

    # Decision variables: x[i,j] = 1 if robot travels from i to j
    x = {}
    for i in range(n):
        for j in range(n):
            if i != j:
                x[i, j] = solver.BoolVar(f'x_{i}_{j}')

    # MTZ subtour elimination variables
    u = [solver.NumVar(0, n, f'u_{i}') for i in range(n)]

    # Objective: maximize packets collected
    objective = solver.Objective()
    for i in range(1, n):  # skip depot
        for j in range(n):
            if i != j:
                objective.SetCoefficient(x[i, j], p[i])
    objective.SetMaximization()

    # 1. No self-travel
    for i in range(n):
        if (i, i) in x:
            solver.Add(x[i, i] == 0)

    # 2. At most one outgoing arc per node
    for i in range(n):
        solver.Add(sum(x[i, j] for j in range(n) if i != j) <= 1)

    # 3. At most one incoming arc per node
    for j in range(n):
        solver.Add(sum(x[i, j] for i in range(n) if i != j) <= 1)

    # 4. Battery constraint: total distance + return to depot <= battery
    total_travel = solver.Sum(x[i, j] * scaled_Cost[i][j] for i in range(n) for j in range(n) if i != j)
    return_to_depot = solver.Sum(x[i, 0] * scaled_Cost[i][0] for i in range(1, n))
    solver.Add(total_travel + return_to_depot <= scaled_battery)

    # 5. MTZ Subtour elimination
    for i in range(1, n):
        for j in range(1, n):
            if i != j:
                solver.Add(u[i] - u[j] + n * x[i, j] <= n - 1)

    # Solve
    status = solver.Solve()
    if status == pywraplp.Solver.OPTIMAL:
        print("Objective value:", objective.Value())
        route = [(i, j) for i, j in x if x[i, j].solution_value() > 0.5]
        print("Edges in optimal route:", route)

    else:
        print("No optimal solution found.")

def solve_robot_network_ilp(Cost, p, battery_capacity):
    n = len(Cost)
    # Scale distances to battery units like Java (/36)
    scaled_Cost = [[Cost[i][j] / 36 for j in range(n)] for i in range(n)]
    scaled_battery = battery_capacity

    solver = pywraplp.Solver.CreateSolver('SCIP')  # Free solver

    # Decision variables: x[i,j] = 1 if robot travels from i to j
    x = {}
    for i in range(n):
        for j in range(n):
            if i != j:
                x[i, j] = solver.BoolVar(f'x_{i}_{j}')

    # MTZ subtour elimination variables
    u = [solver.NumVar(0, n, f'u_{i}') for i in range(n)]

    # Objective: maximize packets collected
    objective = solver.Objective()
    for i in range(1, n):  # skip depot
        for j in range(n):
            if i != j:
                objective.SetCoefficient(x[i, j], p[i])
    objective.SetMaximization()

    # 1. No self-travel
    for i in range(n):
        if (i, i) in x:
            solver.Add(x[i, i] == 0)

    # 2. At most one outgoing arc per node
    for i in range(n):
        solver.Add(sum(x[i, j] for j in range(n) if i != j) <= 1)

    # 3. At most one incoming arc per node
    for j in range(n):
        solver.Add(sum(x[i, j] for i in range(n) if i != j) <= 1)

    # 4. Battery constraint: total distance + return to depot <= battery
    total_travel = solver.Sum(x[i, j] * scaled_Cost[i][j] for i in range(n) for j in range(n) if i != j)
    # Return-to-depot cost if robot leaves a node i
    return_to_depot = solver.Sum(x[i, 0] * scaled_Cost[i][0] for i in range(1, n))
    solver.Add(total_travel + return_to_depot <= scaled_battery)

    # 5. MTZ Subtour elimination
    for i in range(1, n):
        for j in range(1, n):
            if i != j:
                solver.Add(u[i] - u[j] + n * x[i, j] <= n - 1)

    # Solve
    status = solver.Solve()
    if status == pywraplp.Solver.OPTIMAL:
        route = [(i, j) for i, j in x if x[i, j].solution_value() > 0.5]
        total_distance = sum(Cost[i][j] for i, j in route)
        # add return-to-depot distance if any node goes back to depot
        total_distance += sum(Cost[i][0] for i in range(1, n) if x[i,0].solution_value() > 0.5)
        
        print("Objective value (packets):", objective.Value())
        print("Edges in optimal route:", route)
        print("Total distance traveled:", total_distance)

    else:
        print("No optimal solution found.")

# ================= Example usage =================
Cost = [
[9999.99, 539.30, 1091.30, 461.88, 939.76, 511.20, 975.63, 664.40, 615.22, 673.59, 706.12, 620.26, 825.00, 870.70, 950.88, 478.02, 900.89, 710.63, 877.23, 622.11],
[539.30, 9999.99, 603.33, 256.14, 409.93, 410.33, 644.16, 199.58, 333.21, 257.16, 341.30, 478.77, 322.85, 536.48, 478.57, 303.43, 665.65, 418.96, 404.29, 297.98],
[1091.30, 603.33, 9999.99, 843.44, 249.18, 680.95, 981.96, 429.77, 850.89, 419.66, 789.03, 633.96, 280.49, 370.87, 142.52, 644.50, 531.85, 467.33, 214.14, 492.15],
[461.88, 256.14, 843.44, 9999.99, 623.39, 588.31, 526.43, 455.72, 158.71, 511.03, 244.27, 681.16, 566.63, 788.08, 727.46, 486.73, 904.31, 658.44, 655.01, 535.49],
[939.76, 409.93, 249.18, 623.39, 9999.99, 629.29, 736.61, 295.95, 610.64, 328.59, 542.50, 623.94, 146.11, 455.20, 207.42, 557.64, 633.80, 468.00, 192.59, 427.40],
[511.20, 410.33, 680.95, 588.31, 629.29, 9999.99, 1053.20, 346.77, 718.00, 300.83, 747.41, 111.31, 483.84, 377.96, 541.84, 107.35, 390.03, 225.00, 483.74, 202.48],
[975.63, 644.16, 981.96, 526.43, 736.61, 1053.20, 9999.99, 783.66, 367.76, 854.71, 307.33, 1121.64, 788.05, 1114.15, 930.79, 945.86, 1273.75, 1037.81, 884.10, 927.11],
[664.40, 199.58, 429.77, 455.72, 295.95, 346.77, 783.66, 9999.99, 518.77, 71.06, 502.12, 369.69, 162.63, 344.82, 293.82, 264.05, 491.55, 255.20, 218.29, 158.90],
[615.22, 333.21, 850.89, 158.71, 610.64, 718.00, 367.76, 518.77, 9999.99, 584.23, 106.30, 801.29, 591.29, 863.26, 753.65, 612.21, 998.65, 752.13, 687.70, 631.05],
[673.59, 257.16, 419.66, 511.03, 328.59, 300.83, 854.71, 71.06, 584.23, 9999.99, 571.69, 308.46, 183.85, 279.73, 277.91, 234.36, 421.10, 184.46, 2206.16, 100.50],
[706.12, 341.30, 789.03, 244.27, 542.50, 747.41, 307.33, 502.12, 106.30, 571.69, 9999.99, 820.05, 546.01, 844.64, 704.65, 640.08, 992.47, 749.67, 644.38, 633.34],
[620.26, 478.77, 633.96, 681.16, 623.94, 111.31, 1121.64, 369.69, 801.29, 308.46, 820.05, 9999.99, 478.67, 295.79, 502.40, 195.30, 280.86, 167.34, 455.93, 210.83],
[825.00, 322.85, 280.49, 566.63, 146.11, 483.84, 788.05, 162.63, 591.29, 183.85, 546.01, 478.67, 9999.99, 339.78, 162.89, 416.20, 514.42, 326.38, 98.49, 281.60],
[870.70, 536.48, 370.87, 788.08, 455.20, 377.96, 1114.15, 344.82, 863.26, 279.73, 844.64, 295.79, 339.78, 9999.99, 268.99, 393.22, 178.68, 160.08, 262.77, 265.42],
[950.88, 478.57, 142.52, 727.46, 207.42, 541.84, 930.79, 293.82, 753.65, 277.91, 704.65, 502.40, 162.89, 268.99, 9999.99, 502.14, 444.89, 335.09, 75.58, 350.10],
[478.02, 303.43, 644.50, 486.73, 557.64, 107.35, 945.86, 264.05, 612.21, 234.36, 640.08, 195.30, 416.20, 393.22, 502.14, 9999.99, 449.11, 233.45, 435.57, 153.05],
[900.89, 665.65, 531.85, 904.31, 633.80, 390.03, 1273.75, 491.55, 998.65, 421.10, 992.47, 280.86, 514.42, 178.68, 444.89, 449.11, 9999.99, 246.98, 441.28, 369.63],
[710.63, 418.96, 467.33, 658.44, 468.00, 225.00, 1037.81, 255.20, 752.13, 184.46, 749.67, 167.34, 326.38, 160.08, 335.09, 233.45, 246.98, 9999.99, 291.25, 122.98],
[877.23, 404.29, 214.14, 655.01, 192.59, 483.74, 884.10, 218.29, 687.70, 206.16, 644.38, 455.93, 98.49, 262.77, 75.58, 435.57, 441.28, 291.25, 9999.99, 286.01],
[622.11, 297.98, 492.15, 535.49, 427.40, 202.48, 927.11, 158.90, 631.05, 100.50, 633.34, 210.83, 281.60, 265.42, 350.10, 153.05, 369.63, 122.98, 286.01, 9999.99]
]
p = [0, 29, 26, 18, 6, 71, 14, 4, 60, 33, 53, 2, 22, 39, 48, 34, 58, 45, 79, 62]
battery_capacity = 90

solve_robot_network_ilp(Cost, p, battery_capacity)
