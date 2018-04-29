package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;


public class MikasaAI extends AbstractionLayerAI {

    Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType rangedType;

    public MikasaAI(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }
    
    
    public MikasaAI(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public void reset() {
    	super.reset();
    }
    
    public void reset(UnitTypeTable a_utt)  
    {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        lightType = utt.getUnitType("Light");
        rangedType = utt.getUnitType("Ranged");
    }   
    

    public AI clone() {
        return new MikasaAI(utt, pf);
    }
   
   
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
      //Checks to see map size, will change tactics depending on map size
        int BoardHeight = pgs.getHeight();
        int BoardWidth = pgs.getWidth();
        
      
  
        	//Chooses the behaviour for the bases
        	for (Unit u : pgs.getUnits()) {
        		if (u.getType() == baseType
        				&& u.getPlayer() == player
        				&& gs.getActionAssignment(u) == null) {
        			baseBehavior(u, p, pgs, BoardHeight, BoardWidth);
        		}
        	}

        	//Chooses the behaviour for the barracks
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs, BoardHeight, BoardWidth);
            }
        }

      //Chooses the behaviour for the melee units
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                meleeUnitBehavior(u, p, gs);
            }
        }

      //Chooses the behaviour for the worker units
        List<Unit> workers = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) {
                workers.add(u);
            }
        }
        workersBehavior(workers, p, pgs, BoardHeight, BoardWidth);

        //Apparently I need a player action
        return translateActions(player, gs);
        
        
    }
    
    //Defines the Behavior of the bases
    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs, int boardHeight, int boardWidth) {
    	//Checks to see if the map is of a certain size, and will chooses a behaviour based off the width and height values.
    	
    	int nworkers = 0;
        //Gets all my worker units
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
            	nworkers++;
            }
        }
    	//If the map is big it will do this
    	if( boardWidth > 10 && boardHeight > 10)
    	{	
	        //If there are less than 4 workers then we'll train some until we reach 4 workers
	        if (nworkers < 4 && p.getResources() >= workerType.cost) {
	            train(u, workerType);
	        }
	        //if the map is small it will do this
    	}else if(boardWidth <= 10 && boardHeight <= 10)										
    	{
	        for (Unit u2 : pgs.getUnits()) {
	            if (u2.getType() == workerType
	                    && u2.getPlayer() == p.getID()) {
	            }
	        }
	        //Trains workers until there are no available workers, for a worker rush
	        if (nworkers < 3 && p.getResources() >= workerType.cost) {
	            train(u, workerType);
	        }
    	}
    }
    //Defines the bahaviour for the barracks. We only have barracks on larger maps currently
    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs, int boardHeight, int boardWidth) {
    	//This check is in incase a strategy with barracks on smaller maps is developed.
    	if(boardHeight > 10 && boardWidth > 10)
    	{
	    	int nLight = 0;
	    	//Gets all my currently trained Light units
	    	for (Unit u2 : pgs.getUnits()) {
	            if (u2.getType() == lightType
	                    && u2.getPlayer() == p.getID()) {
	                nLight++;
	            }
	        }
	    	//If there are less than 2 of my light units on the board, then we will train them until there are 2
	        if (nLight <= 2 && p.getResources() >= lightType.cost) {
	            train(u, lightType);
	            //Once we've reached 2 light units we will train ranged units
	        }else if (p.getResources() >= rangedType.cost)
	        {
	        	train(u, rangedType);
	        }
	        //If the map is small we will only train ranged units
    	}else {
    		if (p.getResources() >= rangedType.cost)
	        {
	        	train(u, rangedType);
	        }
    	}
    	}
    
    //Defines the attack behaviour for our light and ranged units
    public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        //For each light unit checks for the nearest enemy and the distance to that enemy.
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        //If there is an enemy to rush to, we will rush
        if (closestEnemy != null) {

            attack(u, closestEnemy);
        }
    }
    //Defines the workers behaviour
    public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs, int boardHeight, int boardWidth) {
    	
    	int nbases = 0;
        int nbarracks = 0;
    	//On larger maps it will select the following behaviour
    	if(boardHeight > 10 && boardWidth >10)
    	{
	        
	
	        int resourcesUsed = 0;
	        //Creates a list for worker units, then assigns our workers list to it
	        List<Unit> freeWorkers = new LinkedList<Unit>();
	        freeWorkers.addAll(workers);
	        //If the workers list is empty (We have no workers) then return empty, as we do not need this function to perform anything.
	        if (workers.isEmpty()) {
	            return;
	        }
	     
	        for (Unit u2 : pgs.getUnits()) {
	        	//Records the number of bases we have
	            if (u2.getType() == baseType
	                    && u2.getPlayer() == p.getID()) {
	                nbases++;
	            }
	            //Records the number of barracks we have
	            if (u2.getType() == barracksType
	                    && u2.getPlayer() == p.getID()) {
	                nbarracks++;
	            }
	        }
	
	        List<Integer> reservedPositions = new LinkedList<Integer>();
	        //If we have 1 or less bases and we have the resources to build one then we will
	        if (nbases <= 1 && !freeWorkers.isEmpty()) {
	            // build a base:
	            if (p.getResources() >= baseType.cost + resourcesUsed) {
	                Unit u = freeWorkers.remove(0);
	                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
	                resourcesUsed += baseType.cost;
	            }
	        }
	        //If we have 1 or less barracks and we have the resources to build one then we will
	        if (nbarracks <= 1) {
	            // build a barracks:
	            if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) {
	                Unit u = freeWorkers.remove(0);
	                buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
	                resourcesUsed += barracksType.cost;
	            }
	        }
	        
	
	        // harvest with all the free workers:
	        for (Unit u : freeWorkers) {
	            Unit closestBase = null;
	            Unit closestResource = null;
	            int closestDistance = 0;
	            for (Unit u2 : pgs.getUnits()) {
	            	//Gets the distance to the nearest resource
	                if (u2.getType().isResource) {
	                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	                    if (closestResource == null || d < closestDistance) {
	                        closestResource = u2;
	                        closestDistance = d;
	                    }
	                }
	            }
	            closestDistance = 0;
	            for (Unit u2 : pgs.getUnits()) {
	            	//Gets the distance between the unit and the nearest base
	                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
	                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	                    if (closestBase == null || d < closestDistance) {
	                        closestBase = u2;
	                        closestDistance = d;
	                    }
	                }
	            }
	            //If we have a base and there are resource left that can be mined then we will mine
	            if (closestResource != null && closestBase != null) {
	                AbstractAction aa = getAbstractAction(u);
	                if (aa instanceof Harvest) {
	                    Harvest h_aa = (Harvest)aa;
	                    if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
	                } else {
	                    harvest(u, closestResource, closestBase);
	                }
	            }
	        }
	     //On a small map we will choose this behaviour
    	}else
    	{
	       
	        int resourcesUsed = 0;
	        //Gets the list of workers as in the above behaviour
	        List<Unit> freeWorkers = new LinkedList<Unit>();
	        freeWorkers.addAll(workers);
	        //Checks to see if the list is empty and will break out if it is.
	        if (workers.isEmpty()) {
	            return;
	        }
	        
	        for (Unit u2 : pgs.getUnits()) {
	        	//Gets our number of bases
	            if (u2.getType() == baseType
	                    && u2.getPlayer() == p.getID()) {
	                nbases++;
	            }
	            //Gets number of barracks
	            if (u2.getType() == barracksType
	                    && u2.getPlayer() == p.getID()) {
	                nbarracks++;
	            }
	        }
	        
	        List<Integer> reservedPositions = new LinkedList<Integer>();
	        //We will build one base
	        if (nbases <= 0 && !freeWorkers.isEmpty()) {
	            if (p.getResources() >= baseType.cost + resourcesUsed) {
	                Unit u = freeWorkers.remove(0);
	                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
	                resourcesUsed += baseType.cost;
	            }
	        }
	      //We will build one barracks
	        if (nbarracks <= 0) {
	            // build a barracks:
	            if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) {
	                Unit u = freeWorkers.remove(0);
	                buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
	                resourcesUsed += barracksType.cost;
	            }
	        }
	        
	        //gets each worker unit's closest enemy for a rush tactic
	        for (Unit u : freeWorkers) 
	        {
	        	Unit closestEnemy = null;
	        	int closestEnemyDistance = 0;
	        	for(Unit u2 : pgs.getUnits())
	        	{
	        		if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
	                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	                    if (closestEnemy == null || d < closestEnemyDistance) {
	                        closestEnemy = u2;
	                        closestEnemyDistance = d;
	                    }
	                }
	        	}
	        	
	        
	     // harvest with all the free workers:
	        for (Unit i : freeWorkers) {
	            Unit closestBase = null;
	            Unit closestResource = null;
	            int closestResourceDistance = 0;
	            for (Unit u2 : pgs.getUnits()) {
	                if (u2.getType().isResource) {
	                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	                    if (closestResource == null || d < closestResourceDistance) {
	                        closestResource = u2;
	                        closestResourceDistance = d;
	                    }
	                }
	            }
	            closestResourceDistance = 0;
	            for (Unit u2 : pgs.getUnits()) {
	                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
	                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	                    if (closestBase == null || d < closestResourceDistance) {
	                        closestBase = u2;
	                        closestResourceDistance = d;
	                    }
	                }
	            }
	            if (closestResource != null && closestBase != null) {
	                AbstractAction aa = getAbstractAction(u);
	                if (aa instanceof Harvest) {
	                    Harvest h_aa = (Harvest)aa;
	                    if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
	                } else {
	                    harvest(u, closestResource, closestBase);
	                }
	            }
	        
	        }
	        }
    	}
    	
    }
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }    
    
}
