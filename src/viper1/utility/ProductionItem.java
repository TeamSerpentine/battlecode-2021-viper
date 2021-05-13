package viper1.utility;

import battlecode.common.RobotType;
import battlecode.common.Direction;

    /*
    Class to create objects for the production queue so
    we can not only keep track of what unit, also what direction
    to spawn it on and how strong
     */

public class ProductionItem {
    // what type of robot to build
    private RobotType type;
    // how much influence to put into it. If negative, calculate it at the time of building
    private int influence;
    // what side to (preferably) spawn this unit on, 0 to 7 directions, null means any side
    private Direction dir;

    public ProductionItem(RobotType type, int influence, Direction dir){
        this.type = type;
        this.influence = influence;
        this.dir = dir;
    }

    // side doesn't matter
    public ProductionItem(RobotType type, int influence){
        this(type, influence, null);
    }

    // side and influence doesn't matter
    public ProductionItem(RobotType type){
        this(type, -1, null);
    }

    public RobotType getType(){
        return type;
    }

    public int getInfluence(){
        return influence;
    }

    public Direction getDirection(){
        return dir;
    }
}
