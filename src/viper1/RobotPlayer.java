package viper1;
import battlecode.common.*;
import viper1.robots.EnlightenmentCenter;
import viper1.robots.Muckraker;
import viper1.robots.Politician;
import viper1.robots.Slanderer;

public strictfp class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Depending on the type, create the robot class of that type
        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER: new EnlightenmentCenter(rc); break;
            case POLITICIAN:           new Politician(rc);          break;
            case SLANDERER:            new Slanderer(rc);           break;
            case MUCKRAKER:            new Muckraker(rc);           break;
        }
    }
}
