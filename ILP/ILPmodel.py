from pyomo.environ import *

# Define model
model = ConcreteModel()

# Number of nodes (from OPL model)
n = 10
nodes = range(n)
node = range(1, n)

# Battery Capacity Constraint (Adjustable for vehicle limits)
battery_capacity = 10000

# Cost matrix and prize values (from BC-PCTSP.dat)
Cost = [
    [9999.99, 468.78, 1355.41, 369.73, 2534.35, 2057.14, 2172.91, 3407.69, 223.23, 3838.05],
    [468.78, 9999.99, 914.85, 813.02, 2164.53, 1640.35, 2150.88, 3324.81, 592.0, 3699.48],
    [1355.41, 914.85, 9999.99, 1718.53, 1315.72, 737.41, 2003.6, 2948.82, 1505.93, 3199.89],
    [369.73, 813.02, 1718.53, 9999.99, 2902.2, 2425.97, 2413.64, 3665.44, 240.29, 4122.06],
    [2534.35, 2164.53, 1315.72, 2902.2, 9999.99, 628.95, 1858.38, 2202.46, 2725.02, 2234.57],
    [2057.14, 1640.35, 737.41, 2425.97, 628.95, 9999.99, 1997.72, 2642.49, 2225.26, 2768.77],
    [2172.91, 2150.88, 2003.6, 2413.64, 1858.38, 1997.72, 9999.99, 1256.59, 2390.01, 1756.64],
    [3407.69, 3324.81, 2948.82, 3665.44, 2202.46, 2642.49, 1256.59, 9999.99, 3628.3, 577.59],
    [223.23, 592.0, 1505.93, 240.29, 2725.02, 2225.26, 2390.01, 3628.3, 9999.99, 4061.07],
    [3838.05, 3699.48, 3199.89, 4122.06, 2234.57, 2768.77, 1756.64, 577.59, 4061.07, 9999.99]
]

p = [0, 10, 20, 15, 25, 30, 22, 18, 14, 12]

# Decision Variables
model.dvar = Var(nodes, nodes, within=Binary)  # Binary decision variable
model.u = Var(nodes, within=NonNegativeReals)  # Rank variable for subtour elimination

# Objective Function: Maximize collected prizes while minimizing travel cost
model.obj = Objective(
    expr=sum(p[i] * sum(model.dvar[i, j] for j in nodes) for i in node) -
         sum(Cost[i][j] * model.dvar[i, j] for i in nodes for j in nodes),
    sense=maximize
)

# Constraints
model.self_travel = ConstraintList()
for i in nodes:
    model.self_travel.add(model.dvar[i, i] == 0)

model.one_out = ConstraintList()
for i in nodes:
    model.one_out.add(sum(model.dvar[i, j] for j in nodes) <= 1)

model.one_in = ConstraintList()
for j in nodes:
    model.one_in.add(sum(model.dvar[i, j] for i in nodes) <= 1)

# Battery Constraint: Limit total travel cost
model.battery_limit = Constraint(
    expr=sum(Cost[i][j] * model.dvar[i, j] for i in nodes for j in nodes) <= battery_capacity
)

# Solve using an ILP solver (e.g., CBC, Gurobi, or CPLEX)
solver = SolverFactory('gurobi')
solver.solve(model, tee=True)

# Debugging Output
print("\n--- Model Summary ---")
model.pprint()

print("\n--- Solution Summary ---")
print("Objective Value:", value(model.obj))

print("\n--- Decision Variables ---")
for i in nodes:
    for j in nodes:
        print(f'dvar[{i},{j}] =', value(model.dvar[i, j]))
