import subprocess
import sys
from collections import defaultdict
import os
import traceback
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading

# Configuration
#MAPS = ["Basic1", "Basic2", "Basic3", "Bedwars", "CaveInForest", "CreeperAwMan", "DenseForest", "EmotionalSupport", "Empty", "EndIsland", "Face1", "Herobrine", "LakeFight", "MinecraftVillage", "NarrowCave", "Rainforest", "ResourceTrip", "Shapes", "TheTowers"]
MAPS = ["Bedrooms", "BigGrid", "BlueCross", "Compartments", "DiamondBeacon", "Grid", "H", "Honest1v1", "Islands", "LearnToSwim", "Messy", "Momo", "MoreSquares", "November", "Rivers", "SimpleSpiral", "Skyblock", "SmallGrid", "Squares", "Steve", "Stretched", "Swamp", "TicTacToe", "Virus", "Voidy", "Waterworld", "Wobbly", "X", "Zigzag", "srawdeB"]
#MAPS = ["Basic1", "Basic2", "Basic3", "Bedwars", "CaveInForest", "CreeperAwMan", "DenseForest", "EmotionalSupport", "Empty", "EndIsland", "Face1", "Herobrine", "LakeFight", "MinecraftVillage", "NarrowCave", "Rainforest", "ResourceTrip", "Shapes", "TheTowers", "Bedrooms", "BigGrid", "BlueCross", "Compartments", "DiamondBeacon", "Grid", "H", "Honest1v1", "Islands", "LearnToSwim", "Messy", "Momo", "MoreSquares", "November", "Rivers", "SimpleSpiral", "Skyblock", "SmallGrid", "Squares", "Steve", "Stretched", "Swamp", "TicTacToe", "Virus", "Voidy", "Waterworld", "Wobbly", "X", "Zigzag", "srawdeB"]
#MAPS = ["Swamp", "SimpleSpiral", "TicTacToe", "Virus"]
OFFSET = 1000
SEED_RANGE = range(OFFSET, OFFSET+10)  # left to right-1
MAX_WORKERS = 8  # Number of parallel matches to run (reduce if you see failures)

# Thread-safe counter for match IDs
match_counter = 0
counter_lock = threading.Lock()

def get_next_match_id():
    """Get the next unique match ID in a thread-safe way."""
    global match_counter
    with counter_lock:
        match_counter += 1
        return match_counter

def run_match(package1, package2, map_name, seed):
    """Run a single match and extract the winner from stdout."""
    match_id = get_next_match_id()
    outfile = f"{map_name}-{package1}-{package2}-{seed}"
    
    try:
        # Build the ant command
        cmd = [
            'ant', 'run_test',
            f'-Dpackage1={package1}',
            f'-Dpackage2={package2}',
            f'-Dmap={map_name}',
            f'-Dseed={seed}',
            f'-Doutfile={outfile}'
        ]
        
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            shell=True,
            timeout=300  # 5 minute timeout
        )
        
        # Check if there was a ClassNotFoundException
        if "ClassNotFoundException" in result.stdout:
            print(f"\nERROR: ClassNotFoundException in Map={map_name}, Seed={seed}")
            print("STDOUT:\n", result.stdout)
            return map_name, seed, None
        
        # Find the line containing "Winner:" in stdout
        for line in result.stdout.split('\n'):
            if "Winner:" in line:
                # Parse "[java] Winner: NAME (Team X)"
                start = line.find("Winner:") + 7
                end = line.find("(Team", start)
                if start > 6 and end > start:
                    winner = line[start:end].strip()
                    return map_name, seed, winner
        
        print(f"Warning: Could not find 'Winner:' in stdout for Map={map_name}, Seed={seed}")
        return map_name, seed, None
        
    except subprocess.TimeoutExpired:
        print(f"Error: Match {match_id} (Map={map_name}, Seed={seed}) timed out")
        return map_name, seed, None
    except Exception as e:
        print(f"Error running match {match_id}: {e}")
        traceback.print_exc()
        return map_name, seed, None

def build_packages(package1, package2):
    """Build both packages once before running matches."""
    print(f"Building {package1} and {package2}...")
    try:
        result = subprocess.run(
            ['ant', 'build', f'-Dpackage1={package1}', f'-Dpackage2={package2}'],
            capture_output=True,
            text=True,
            shell=True,
            timeout=120
        )
        if result.returncode != 0:
            print(f"Warning: Build returned code {result.returncode}")
            print("STDOUT:", result.stdout[-500:])  # Last 500 chars
            print("STDERR:", result.stderr[-500:])
        else:
            print("Build successful!")
    except Exception as e:
        print(f"Error building: {e}")
        traceback.print_exc()
    
    print()

def main():
    try:
        if len(sys.argv) != 3:
            print("Usage: python script.py <package1> <package2>")
            sys.exit(1)
        
        os.environ["JAVA_HOME"] = r"C:\Users\jonca\.jdks\temurin-1.8.0_462"
        os.environ["PATH"] = os.path.join(os.environ["JAVA_HOME"], "bin") + ";" + os.environ["PATH"]

        package1 = sys.argv[1]
        package2 = sys.argv[2]
        
        # Build packages once before running matches
        build_packages(package1, package2)
        
        # Statistics tracking
        stats = {
            package1: defaultdict(lambda: {'wins': 0, 'total': 0}),
            package2: defaultdict(lambda: {'wins': 0, 'total': 0})
        }
        total_games = 0
        total_wins = {package1: 0, package2: 0}
        
        print(f"Starting battles between {package1} and {package2}")
        print(f"Maps: {MAPS}")
        print(f"Seeds: {list(SEED_RANGE)}")
        print(f"Running {MAX_WORKERS} matches in parallel")
        print("=" * 60)
        
        # Create list of all matches to run
        matches = [(package1, package2, map_name, seed) 
                   for map_name in MAPS 
                   for seed in SEED_RANGE]
        
        # Run matches in parallel
        with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
            # Submit all matches
            future_to_match = {
                executor.submit(run_match, *match): match 
                for match in matches
            }
            
            # Process completed matches
            for future in as_completed(future_to_match):
                match = future_to_match[future]
                map_name, seed, winner = future.result()
                
                print(f"Completed: Map={map_name}, Seed={seed}, Winner={winner}")
                
                if winner:
                    total_games += 1
                    
                    # Update statistics
                    stats[package1][map_name]['total'] += 1
                    stats[package2][map_name]['total'] += 1
                    
                    if winner == package1:
                        stats[package1][map_name]['wins'] += 1
                        total_wins[package1] += 1
                    elif winner == package2:
                        stats[package2][map_name]['wins'] += 1
                        total_wins[package2] += 1
                    else:
                        print(f"Warning: Winner '{winner}' doesn't match either package")
        
        # Print results
        print("\n" + "=" * 60)
        print("RESULTS")
        print("=" * 60)
        
        for package in [package1, package2]:
            print(f"\n{package}:")
            print("-" * 40)
            
            for map_name in MAPS:
                map_stats = stats[package][map_name]
                if map_stats['total'] > 0:
                    winrate = (map_stats['wins'] / map_stats['total']) * 100
                    print(f"  {map_name}: {map_stats['wins']}/{map_stats['total']} wins ({winrate:.1f}%)")
            
            if total_games > 0:
                global_winrate = (total_wins[package] / total_games) * 100
                print(f"  GLOBAL: {total_wins[package]}/{total_games} wins ({global_winrate:.1f}%)")
    except Exception as e:
        print("ERROR:", e)
        traceback.print_exc()


if __name__ == "__main__":
    main()