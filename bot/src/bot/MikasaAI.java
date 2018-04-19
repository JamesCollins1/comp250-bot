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
        
      
  
        	// behavior of bases:
        	for (Unit u : pgs.getUnits()) {
        		if (u.getType() == baseType
        				&& u.getPlayer() == player
        				&& gs.getActionAssignment(u) == null) {
        			baseBehavior(u, p, pgs, BoardHeight, BoardWidth);
        		}
        	}

        // behavior of barracks:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs, BoardHeight, BoardWidth);
            }
        }

        // behavior of melee units:
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                meleeUnitBehavior(u, p, gs);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) {
                workers.add(u);
            }
        }
        workersBehavior(workers, p, pgs, BoardHeight, BoardWidth);

        // This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
        return translateActions(player, gs);
        
        
    }

    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs, int boardHeight, int boardWidth) {
    	if( boardWidth > 10 && boardHeight > 10)
    	{	
	        int nworkers = 0;
	        for (Unit u2 : pgs.getUnits()) {
	            if (u2.getType() == workerType
	                    && u2.getPlayer() == p.getID()) {
	            	nworkers++;
	            }
	        }
	        if (nworkers < 4 && p.getResources() >= workerType.cost) {
	            train(u, workerType);
	        }
    	}else if(boardWidth <= 10 && boardHeight <= 10)										//There could be a bug here for irregular shaped maps such as 12X8
    	{
	        for (Unit u2 : pgs.getUnits()) {
	            if (u2.getType() == workerType
	                    && u2.getPlayer() == p.getID()) {
	            }
	        }
	        if (p.getResources() >= workerType.cost) {
	            train(u, workerType);
	        }
    	}
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs, int boardHeight, int boardWidth) {
    	if(boardHeight > 10 && boardWidth > 10)
    	{
	    	int nLight = 0;
	    	for (Unit u2 : pgs.getUnits()) {
	            if (u2.getType() == lightType
	                    && u2.getPlayer() == p.getID()) {
	                nLight++;
	            }
	        }
	    	
	        if (nLight <= 2 && p.getResources() >= lightType.cost) {
	            train(u, lightType);
	      
	        }else if (p.getResources() >= rangedType.cost)
	        {
	        	train(u, rangedType);
	        }
    	}else
    	{
    		
    	}
    }

    public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        //If there is an enemy to rush to, and we have more than half health attack
        if (closestEnemy != null) {

            attack(u, closestEnemy);
        }
    }

    public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs, int boardHeight, int boardWidth) {
    	if(boardHeight > 10 && boardWidth >10)
    	{
	        int nbases = 0;
	        int nbarracks = 0;
	
	        int resourcesUsed = 0;
	        List<Unit> freeWorkers = new LinkedList<Unit>();
	        freeWorkers.addAll(workers);
	
	        if (workers.isEmpty()) {
	            return;
	        }
	
	        for (Unit u2 : pgs.getUnits()) {
	            if (u2.getType() == baseType
	                    && u2.getPlayer() == p.getID()) {
	                nbases++;
	            }
	            if (u2.getType() == barracksType
	                    && u2.getPlayer() == p.getID()) {
	                nbarracks++;
	            }
	        }
	
	        List<Integer> reservedPositions = new LinkedList<Integer>();
	        if (nbases <= 1 && !freeWorkers.isEmpty()) {
	            // build a base:
	            if (p.getResources() >= baseType.cost + resourcesUsed) {
	                Unit u = freeWorkers.remove(0);
	                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
	                resourcesUsed += baseType.cost;
	            }
	        }
	
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
	        	Unit closestEnemy = null;
	            Unit closestBase = null;
	            Unit closestResource = null;
	            int closestDistance = 0;
	            for (Unit u2 : pgs.getUnits()) {
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
	                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
	                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	                    if (closestBase == null || d < closestDistance) {
	                        closestBase = u2;
	                        closestDistance = d;
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
    	}else
    	{
    		int nbases = 0;
	        int nWorkerstoMine = 4;
	        int resourcesUsed = 0;
	        
	        List<Unit> freeWorkers = new LinkedList<Unit>();
	        freeWorkers.addAll(workers);
	        
	        if (workers.isEmpty()) {
	            return;
	        }
	        
	        for (Unit u2 : pgs.getUnits()) {
	            if (u2.getType() == baseType
	                    && u2.getPlayer() == p.getID()) {
	                nbases++;
	            }
	        }
	        
	        List<Integer> reservedPositions = new LinkedList<Integer>();
	        if (nbases <= 2 && !freeWorkers.isEmpty()) {
	            // build a base:
	            if (p.getResources() >= baseType.cost + resourcesUsed) {
	                Unit u = freeWorkers.remove(0);
	                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
	                resourcesUsed += baseType.cost;
	            }
	        }
	        
	        
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
	            if(closestEnemyDistance < 5)
	            {
	            	attack(u, closestEnemy);
	            }else if (closestResource != null && closestBase != null) {
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
