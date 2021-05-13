package viper1.utility;

import battlecode.common.Direction;
import battlecode.common.RobotType;

public class Constants {

    public static final RobotType[] SPAWNABLE_ROBOT = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };

    public static final Direction[] DIRECTIONS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    // variables for communication
    // Codes <= 500 are reserved for neutral HQ signalling
    public static final int NBITS = 7; //number of bits per coordinate
    public static final int BITMASK = (1 << NBITS) - 1;
    public static final int EnemyHQcode = 745;
    public static final int ClearCode = 836;
    public static final int DefensiveCode = 987;
    public static final int neutralHQcap = 500; //max influence/conviction a neutral hq can have
    public static final int Scoutingcode = 565; // currently not in use, will be used for sending scouts to a location
}
