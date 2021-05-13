# Battlecode 2021: Serpentine Viper
<img src="https://serpentine.ai/wp-content/uploads/2019/02/Final-design-serpentine.png" width="400px">

Battlecode is a yearly competition hosted by MIT, every year with a different 'game'.<br/>
This year the game is about movable units, each with a special power, who wanted to win 'the election' of their political party. A team can win if they, after 1500 rounds, have bought the most votes, with their currency 'influence'. But it could also be won if all movable units and the enlightenment centres of the enemy team have been defeated.<br/>
Four main strategies were implemented by team Viper. Information between units can be shared using flags across the map to the enlightenment centre. So, a flag system was implemented to share information with other units across the map, in order for new units/robots to get specific tasks, and to get the location of that specific task. Some units had the main task to stay alive and run away from robots of the opposite team. Another strategy was to send a specific kind of robot, the politician, to neutral or enemy enlightenment centres to concur and convert them. One kind of robot 'cheap' to make, the muckrakers, so they were used to eliminate enemy robots and to explore the map.<br/>
With this strategy team Viper came in 82nd place, with an elo score of 1336. There were a total of 150 wins and 144 losses for Team Viper. The time constraint is one of the challenges that team viper struggled with. Participants should be strictly organised and prepared to fully utilise the time given.<br/>

<img src="https://serpentine.ai/wp-content/uploads/2021/05/viperl.png" width="400px">

# Battlecode 2021 Scaffold

This is the Battlecode 2021 scaffold, containing an `examplefuncsplayer`. Read https://2021.battlecode.org/getting-started!

### Project Structure

- `README.md`
    This file.
- `build.gradle`
    The Gradle build file used to build and run players.
- `src/`
    Player source code.
- `test/`
    Player test code.
- `client/`
    Contains the client. The proper executable can be found in this folder (don't move this!)
- `build/`
    Contains compiled player code and other artifacts of the build process. Can be safely ignored.
- `matches/`
    The output folder for match files.
- `maps/`
    The default folder for custom maps.
- `gradlew`, `gradlew.bat`
    The Unix (OS X/Linux) and Windows versions, respectively, of the Gradle wrapper. These are nifty scripts that you can execute in a terminal to run the Gradle build tasks of this project. If you aren't planning to do command line development, these can be safely ignored.
- `gradle/`
    Contains files used by the Gradle wrapper scripts. Can be safely ignored.


### Useful Commands

- `./gradlew run`
    Runs a game with the settings in gradle.properties
- `./gradlew update`
    Update to the newest version! Run every so often

