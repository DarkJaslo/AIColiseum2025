import subprocess
import sys
from collections import defaultdict
import os
import traceback

# Configuration
#MAPS = ["Basic1", "Basic2", "Basic3", "Bedwars", "CaveInForest", "CreeperAwMan", "DenseForest", "EmotionalSupport", "Empty", "EndIsland", "Face1", "Herobrine", "LakeFight", "MinecraftVillage", "Rainforest", "Shapes", "TheTowers", "ResourceTrip", "NarrowCave"]
MAPS = ["NarrowCave"]
OFFSET = 10000
SEED_RANGE = range(OFFSET, OFFSET+10)  # left to right-1
BUILD_FILE = "build.defaults"

def modify_build_file(package1, package2, map_name, seed):
    """Modify the build.defaults file with the given parameters."""
    content = f"""package1={package1}
package2={package2}
map={map_name}
seed={seed}
"""
    with open(BUILD_FILE, 'w') as f:
        f.write(content)

def run_ant_and_get_winner():
    """Run 'ant run' and extract the winner from output."""
    try:
        result = subprocess.run(
            ['ant', 'run'],
            capture_output=True,
            text=True,
            shell=True,
            timeout=300  # 5 minute timeout
        )

        #print("STDOUT BEGIN")
        #print(result.stdout)
        #print("STDOUT END")
        
        # Find the line containing "Winner:"
        for line in result.stdout.split('\n'):
            if "Winner:" in line:
                # Parse "[java] Winner: NAME (Team X)"
                start = line.find("Winner:") + 7
                end = line.find("(Team", start)
                if start > 6 and end > start:
                    winner = line[start:end].strip()
                    return winner
        
        print(f"Warning: Could not find 'Winner:' in output")
        print("STDOUT BEGIN")
        print(result.stdout)
        print("STDOUT END")
        return None
        
    except subprocess.TimeoutExpired:
        print("Error: Command timed out")
        return None
    except Exception as e:
        print(f"Error running ant: {e}")
        traceback.print_exc()
        return None

def main():
    try:
        if len(sys.argv) != 3:
            print("Usage: python script.py <package1> <package2>")
            sys.exit(1)
        
        os.environ["JAVA_HOME"] = r"C:\Users\jonca\.jdks\temurin-1.8.0_462"
        os.environ["PATH"] = os.path.join(os.environ["JAVA_HOME"], "bin") + ";" + os.environ["PATH"]

        package1 = sys.argv[1]
        package2 = sys.argv[2]
        
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
        print("=" * 60)
        
        # Run all combinations
        for map_name in MAPS:
            for seed in SEED_RANGE:
                print(f"\nRunning: Map={map_name}, Seed={seed}...", end=" ")
                
                # Modify build file
                modify_build_file(package1, package2, map_name, seed)
                
                # Run ant and get winner
                winner = run_ant_and_get_winner()
                
                if winner:
                    print(f"Winner: {winner}")
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
                else:
                    print("FAILED")
        
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
        print("ERROR:",e)
        traceback.print_exc()


if __name__ == "__main__":
    main()