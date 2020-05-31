# Solution

## Execution

### Program

#### nix

From project root directory:

```bash
$ nix-shell --pure --run "sbt \"run <directory>\""
```

#### sbt

From project root directory:

```bash
$ sbt "run <directory>"
```

### Tests


#### nix

From project root directory:

```bash
$ nix-shell --pure --run "sbt test"
```

#### sbt

From project root directory:

```bash
$ sbt test
```

## Requirements

| Requirement | Implemented? | Comment |
| :--- | :---: | :--- |
| Program prints statistics to StdOut| Yes | - |
| It reports how many files it processed | Yes | - |
| It reports how many measurements it processed | Yes | - |
| It reports how many measurements failed | Yes | - |
| For each sensor it calculates min/avg/max humidity | Yes | - |
| `NaN` values are ignored from min/avg/max | Yes | - |
| Sensors with only `NaN` measurements have min/avg/max as `NaN/NaN/NaN` | Yes | - |
| Program sorts sensors by highest avg humidity (`NaN` values go last) | Yes | - |
| Safely read large files | Yes | Achieved with `fs2.io.file.readAll` |
| No disk/database writes | Yes | - |
| No Spark dependency     | Yes | Combination of \[`fs2` \| `cats`\|`Monoid`\|`Semigroup`\] |
| Purely functional program | Almost | Not all `effects` are handled (e.g. unsafe `String` to `Int` conversion) |
| Tests | Yes | `cats-laws` + `solution` tests |

## Assumptions

1. Program doesn't parse headers, so in case of permutation (e.g. `humidity` first, `sensor-id` second) it will inevitably fail - no exceptions but it won't show meaningful results.
1. When any entry within daily report file doesn't follow the convention `<sensor-id>,(\d+)` (achieved as combination of `String` splitting and regex matching) program will report such line as one associated with sensor identified by `measure-error` id.
1. Since program cannot write results to a database/disk it means that total amount of sensors (and their aggregated data) should fit in a memory, therefore `Map` structure was used in order to aggregate results.

## Area for improvement

1. Better error handling. For now it doesn't exist but it seems that nothing is thrown.
1. Better `NaN` handling. For now it was done in a dirty way, utilizing `Option` type. It works but it is a little bit nasty code. Maybe ADT would make this code better but...
1. In case of a large files, GC overhead may be significant. Program maps each line to some case classes and such objects creation may cause performance issues.
1. Better console output - colors etc.
1. Setup sbt compiler flags.
1. Move hardcoded values to config file (e.g. chunk size).
1. Add logger.


# Sensor Statistics Task

Create a command line program that calculates statistics from humidity sensor data.

### Background story

The sensors are in a network, and they are divided into groups. Each sensor submits its data to its group leader.
Each leader produces a daily report file for a group. The network periodically re-balances itself, so the sensors could
change the group assignment over time, and their measurements can be reported by different leaders. The program should
help spot sensors with highest average humidity.

## Input

- Program takes one argument: a path to directory
- Directory contains many CSV files (*.csv), each with a daily report from one group leader
- Format of the file: 1 header line + many lines with measurements
- Measurement line has sensor id and the humidity value
- Humidity value is integer in range `[0, 100]` or `NaN` (failed measurement)
- The measurements for the same sensor id can be in the different files

### Example

leader-1.csv
```
sensor-id,humidity
s1,10
s2,88
s1,NaN
```

leader-2.csv
```
sensor-id,humidity
s2,80
s3,NaN
s2,78
s1,98
```

## Expected Output

- Program prints statistics to StdOut
- It reports how many files it processed
- It reports how many measurements it processed
- It reports how many measurements failed
- For each sensor it calculates min/avg/max humidity
- `NaN` values are ignored from min/avg/max
- Sensors with only `NaN` measurements have min/avg/max as `NaN/NaN/NaN`
- Program sorts sensors by highest avg humidity (`NaN` values go last)

### Example

```
Num of processed files: 2
Num of processed measurements: 7
Num of failed measurements: 2

Sensors with highest avg humidity:

sensor-id,min,avg,max
s2,78,82,88
s1,10,54,98
s3,NaN,NaN,NaN
```

## Notes

- Single daily report file can be very large, and can exceed program memory
- Program should only use memory for its internal state (no disk, no database)
- Any open source library can be used (besides Spark) 
- Please use vanilla scala, akka-stream, monix or similar technology. 
- You're more than welcome to implement a purely functional solution using cats-effect, fs2 and/or ZIO to impress, 
  but this is not a mandatory requirement. 
- Sensible tests are welcome
