package MapGeneration;


import MapGeneration.DataExport.MapPrinter;
import MapGeneration.GenerationSettings.Options;
import MapGeneration.Graph.Polygon;
import MapGeneration.Graph.PolygonProperties.*;


import java.util.*;

public class Map {
    public VoronoiDiagram diagram;
    MapPrinter generatedMap;
    Options settings;
    public MapPrinter getMap(VoronoiDiagram diagram)
    {
        this.diagram = diagram;
        generateMap();
        return generatedMap;
    }
    public MapPrinter getMap()
    {
        generateMap();
        return generatedMap;
    }
    public Map(Options settings)
    {
        this.settings = settings;
    }

    public void generateDiagram()
    {
        diagram = new VoronoiDiagram(settings.getXSize(),settings.getYSize());
        diagram.generate(settings.getPolygons());
    }
    private void generateMap()
    {
        if(diagram == null)
        {
            generateDiagram();
        }
        generateBodiesOfWater();
        generateElevations();
        generateClimate();
        generateRivers();
        generateBiomes();
        generateCities();
        generatedMap = new MapPrinter(diagram);
    }

    private void generateCities() {
        int cityCounter = 0;
        int maxCityNumber;
        List<Polygon> polygonList = new ArrayList<>();
        for(Polygon polygon: diagram.polygons)
        {
            if(polygon.water == WaterType.Land)
                polygonList.add(polygon);
        }
        maxCityNumber = (int)(settings.getCityModifier()*(polygonList.size()/100));
        Collections.shuffle(polygonList);
        Random random = new Random();
        for(Polygon polygon: polygonList)
        {
            if(polygon.hasCityNeighbour()) continue;
            int chanceEstimation = estimateCityChance(polygon);
            if(chanceEstimation > random.nextInt(100))
            {
                polygon.city = new City(settings.getCityNames().get(cityCounter));
                cityCounter++;
            }
            if(cityCounter >= maxCityNumber || cityCounter >= settings.getCityNames().size()) break;
        }
    }
    private int estimateCityChance(Polygon polygon)
    {
        int chanceEstimation = 0;
        switch(polygon.temperature)
        {
            case Frigid:
                chanceEstimation -=30;
                break;
            case Cold:
                break;
            case Average:
                chanceEstimation +=10;
                break;
            case Hot:
                chanceEstimation +=15;
            case Scorching:
                break;
        }
        switch(polygon.moisture)
        {
            case SuperWet:
                chanceEstimation +=10;
                break;
            case Wet:
                chanceEstimation +=10;
                break;
            case Normal:
                chanceEstimation +=5;
                break;
            case Dry:
                chanceEstimation -=5;
            case SuperDry:
                chanceEstimation -=20;
                break;
        }
        if(polygon.elevation == Elevation.MountainPeaks) chanceEstimation -= 30;
        if(polygon.hasOceanNeighbour()) chanceEstimation += 40;
        else if(polygon.hasLakeNeighbour()) chanceEstimation += 20;
        if(polygon.river == true) chanceEstimation += 30;
            else if(polygon.hasRiverNeighbour()) chanceEstimation +=10;
        return chanceEstimation;
    }

    private void generateBiomes() {
        diagram.polygons.forEach(Polygon::setBiome);
    }

    private void generateClimate() {
        for(Polygon polygon: diagram.polygons)
        {
            calculateTemperature(polygon);
            if(polygon.water != WaterType.Land) polygon.moisture = Moisture.LiterallyWater;
            else if(polygon.getDistanceToWater()*polygon.temperature.ordinal() < 2.0*Math.sqrt((double)diagram.polygons.size())/(double)80*(double)settings.getMoistureClimateModificator()) polygon.moisture = Moisture.SuperWet;
            else if(polygon.getDistanceToWater()*polygon.temperature.ordinal() < 4.0*Math.sqrt((double)diagram.polygons.size())/(double)80*(double)settings.getMoistureClimateModificator()) polygon.moisture = Moisture.Wet;
            else if(polygon.getDistanceToWater()*polygon.temperature.ordinal() < 10.0*Math.sqrt((double)diagram.polygons.size())/(double)80*(double)settings.getMoistureClimateModificator()) polygon.moisture = Moisture.Normal;
            else if(polygon.getDistanceToWater()*polygon.temperature.ordinal() < 13.0*Math.sqrt((double)diagram.polygons.size())/(double)80*(double)settings.getMoistureClimateModificator()) polygon.moisture = Moisture.Dry;
            else  polygon.moisture = Moisture.SuperDry;
        }
    }

    private void calculateTemperature(Polygon polygon) {
        int tempTemperature = calculateClimateTemperature(polygon).ordinal();
        if(tempTemperature == 0);
            else
        if(polygon.elevation == Elevation.MountainPeaks)  tempTemperature = 0;
        else
        {
             if(polygon.elevation == Elevation.Hight) tempTemperature = tempTemperature - 2;
                else if(polygon.elevation == Elevation.Medium) tempTemperature--;
            if(tempTemperature <= 0) tempTemperature = 1;
        }
            polygon.temperature = Temperature.values()[tempTemperature];
    }

    private Temperature calculateClimateTemperature(Polygon polygon) {
        int mapMiddle = settings.getYSize()/2;
        int map1Percent = settings.getYSize()/100;
        Random random = new Random();
        double mod = 1.0-0.1*((double)(random.nextInt(10)-5));
        switch(settings.getClimate())
        {
            case EquatorOnMiddle:
                if(((mapMiddle - map1Percent*5*mod <= polygon.centerPoint.getY() && mapMiddle >= polygon.centerPoint.getY()) || (mapMiddle + map1Percent*5*mod >= polygon.centerPoint.getY() && mapMiddle <= polygon.centerPoint.getY()))) return Temperature.Scorching;
                if(((mapMiddle - map1Percent*18*mod < polygon.centerPoint.getY() && mapMiddle > polygon.centerPoint.getY()) || (mapMiddle + map1Percent*18*mod > polygon.centerPoint.getY() && mapMiddle < polygon.centerPoint.getY()))) return Temperature.Hot;
                if(((mapMiddle - map1Percent*35 < polygon.centerPoint.getY() && mapMiddle > polygon.centerPoint.getY()) || (mapMiddle + map1Percent*35 > polygon.centerPoint.getY() && mapMiddle < polygon.centerPoint.getY()))) return Temperature.Average;
                if(((mapMiddle - map1Percent*45 < polygon.centerPoint.getY() && mapMiddle > polygon.centerPoint.getY()) || (mapMiddle + map1Percent*45 > polygon.centerPoint.getY() && mapMiddle < polygon.centerPoint.getY()))) return Temperature.Cold;
                return Temperature.Frigid;
            case ColdNorth:
                if(settings.getYSize() - map1Percent*12*mod<= polygon.centerPoint.getY()) return Temperature.Scorching;
                if(settings.getYSize() - map1Percent*36*mod <= polygon.centerPoint.getY()) return Temperature.Hot;
                if(settings.getYSize() - map1Percent*70 <= polygon.centerPoint.getY()) return Temperature.Average;
                if(settings.getYSize() - map1Percent*88 <= polygon.centerPoint.getY()) return Temperature.Cold;
                return Temperature.Frigid;
            case ColdSouth:
                if(settings.getYSize() - map1Percent*12<= polygon.centerPoint.getY()) return Temperature.Frigid;
                if(settings.getYSize() - map1Percent*28*mod <= polygon.centerPoint.getY()) return Temperature.Cold;
                if(settings.getYSize() - map1Percent*64*mod <= polygon.centerPoint.getY()) return Temperature.Average;
                if(settings.getYSize() - map1Percent*88*mod <= polygon.centerPoint.getY()) return Temperature.Hot;
                return Temperature.Scorching;
            case UniformTemperature:
                return Temperature.Average;
            default:

        }
        return null;
    }

    private void generateRivers() {
        Queue<Polygon> polygonQueue = new ArrayDeque<>();
        Set<Polygon> polygonSet = new HashSet<>();


        int landPolygonCounter = 0;
        for(Polygon polygon: diagram.polygons)
        {
            if(polygon.water == WaterType.Land) landPolygonCounter++;
            polygon.setPotentialRiverDirection();
        }

        createRiverStartingPositions(polygonQueue, polygonSet, landPolygonCounter);
        while(!polygonQueue.isEmpty())
        {
            Polygon tempPolygon = polygonQueue.poll();
            tempPolygon.river = true;
            if(tempPolygon.moisture.ordinal() < Moisture.SuperWet.ordinal()) tempPolygon.moisture = Moisture.values()[(tempPolygon.moisture.ordinal()+1)];
            if(!polygonSet.contains(tempPolygon.riverDirection))
            {
                polygonSet.add(tempPolygon.riverDirection);
                polygonQueue.add(tempPolygon.riverDirection);
            }

        }

    }

    private void createRiverStartingPositions(Queue<Polygon> polygonQueue, Set<Polygon> polygonSet, int landPolygonCounter) {
        int riverLimit = landPolygonCounter/100 + settings.getRiverCountModificator();
        Random random = new Random();
        int riverCounter = 0;
        ArrayList<Polygon> polygons = new ArrayList(diagram.polygons);
        Collections.shuffle(polygons);
        for(Polygon polygon: polygons)
        {
            if(polygon.water == WaterType.Land
                    && riverCounter < riverLimit
                    && (polygon.temperature != Temperature.Frigid
                    || polygon.elevation == Elevation.MountainPeaks)
                    && polygon.elevation.ordinal() >=2
                    && polygon.moisture.ordinal() >= 2
                    && (double)random.nextInt(polygon.elevation.ordinal() * landPolygonCounter)/(landPolygonCounter) > 0.995)
            {
                riverCounter++;
                polygon.river = true;
                polygonQueue.add(polygon);
                polygonSet.add(polygon);
            }
        }
    }

    private void generateBodiesOfWater() {
        setMapBordersToWater();
        createOcean();
        createLakes();
        setToLakeDistance();
    }

    private void setToLakeDistance() {
        Queue<Polygon> polygonQueue = new ArrayDeque<>();
        Set<Polygon> polygonSet = new HashSet<>();

        findLakesAndSetTheirDistance(polygonQueue, polygonSet);
        while(!polygonQueue.isEmpty())
        {
            Polygon polygon = polygonQueue.poll();
            for(Polygon neigbour: polygon.neighborPolygons)
            {
                if(!polygonSet.contains(neigbour))
                {
                    if(neigbour.water == WaterType.Land)
                    {
                        neigbour.distanceToLake = polygon.distanceToLake+1;
                        polygonSet.add(neigbour);
                        polygonQueue.add(neigbour);
                    }
                }
            }
        }
    }

    private void findLakesAndSetTheirDistance(Queue<Polygon> polygonQueue, Set<Polygon> polygonSet) {
        for(Polygon polygon: diagram.polygons){
            if(polygon.water == WaterType.Lake)
            {
                polygonQueue.add(polygon);
                polygonSet.add(polygon);
                polygon.distanceToLake = 0;
            }
        }
    }

    private void generateElevations() {
        Queue<Polygon> polygonQueue = new ArrayDeque<>();
        Set<Polygon> polygonSet = new HashSet<>();
        getCoastPolygons(polygonQueue, polygonSet);
        calculatePolygonsDistanceToOcean(polygonQueue, polygonSet);
        setElevations(polygonSet);
    }

    private void setElevations(Set<Polygon> polygonSet) {
        int maxDistance = 0;
        int avargeDistanceToOcean = 0;
        for(Polygon polygon: polygonSet)
        {
            if(polygon.distanceToOcean > maxDistance) maxDistance = polygon.distanceToOcean;
            avargeDistanceToOcean += polygon.distanceToOcean;
        }
        avargeDistanceToOcean = avargeDistanceToOcean / polygonSet.size();
        for(Polygon polygon: polygonSet)
        {
            if(polygon.distanceToOcean <= 2) polygon.elevation = Elevation.Low;
            else if(polygon.distanceToOcean < avargeDistanceToOcean+5) polygon.elevation = Elevation.Medium;
            else if(polygon.elevation!=Elevation.MountainPeaks && polygon.distanceToOcean < (maxDistance+8 + avargeDistanceToOcean)/2) polygon.elevation = Elevation.Hight;
            else polygon.elevation = Elevation.MountainPeaks;

        }
    }

    private void calculatePolygonsDistanceToOcean(Queue<Polygon> polygonQueue, Set<Polygon> polygonSet) {
        while(!polygonQueue.isEmpty())
        {
            Polygon polygon = polygonQueue.poll();
            for(Polygon polygonNeigbor:polygon.neighborPolygons)
            {
                if(!polygonSet.contains(polygonNeigbor))
                {
                    polygonNeigbor.distanceToOcean = polygon.distanceToOcean + 1;
                    polygonQueue.add(polygonNeigbor);
                    polygonSet.add(polygonNeigbor);
                }
            }
        }
    }

    private void getCoastPolygons(Queue<Polygon> polygonQueue, Set<Polygon> polygonSet) {
        for(Polygon polygon: diagram.polygons)
        {
            if(polygon.water == WaterType.Land && polygon.hasOceanNeighbour())
            {
                polygon.distanceToOcean = 0;
                polygonQueue.add(polygon);
                polygonSet.add(polygon);
            }
        }
    }

    private void createLakes() {
        Queue<Polygon> polygonQueue = new ArrayDeque<>();
        Set<Polygon> polygonSet = new HashSet<>();
        ArrayList<Polygon> landPolygons = new ArrayList<>();

        for(Polygon polygon: diagram.polygons)
            if(polygon.water == WaterType.Land) landPolygons.add(polygon);
        int lakeLimit = landPolygons.size()/300 + settings.getLakeCountModificator();
        createLakeStartingPoints(polygonQueue, polygonSet, landPolygons, lakeLimit);
        expandLakeSizes(polygonQueue, polygonSet, lakeLimit);
    }

    private void expandLakeSizes(Queue<Polygon> polygonQueue, Set<Polygon> polygonSet, int lakeLimit) {
        int lakeCounter = 0;
        int lakeSizeCounter = 0;
        while(polygonQueue.size()>0)
        {
            Polygon polygon = polygonQueue.poll();
            for(Polygon neighborPolygon: polygon.neighborPolygons)
            {
                if(lakeLimit * settings.getTotalLakeAreaLimitMultipler() < lakeCounter) break;
                if(!polygonSet.contains(neighborPolygon) && neighborPolygon.water == WaterType.Land && !neighborPolygon.hasOceanNeighbour())
                    if(neighborPolygon.getWaterToLandNeighbourRatio() < 1.0-lakeSizeCounter/(double)settings.getLakeSizeLimitModificator())
                    {
                        neighborPolygon.water = WaterType.Lake;
                        lakeCounter++;
                        lakeSizeCounter++;
                        polygonQueue.add(neighborPolygon);
                        polygonSet.add(neighborPolygon);
                    }
            }
        }
    }

    private void createLakeStartingPoints(Queue<Polygon> polygonQueue, Set<Polygon> polygonSet, ArrayList<Polygon> landPolygons, int lakeLimit) {
        int lakeCounter = 0;
        Random random = new Random();

        for(Polygon polygon: landPolygons)
        {
            if(lakeCounter < lakeLimit && !polygon.hasOceanNeighbour() && (double)random.nextInt(landPolygons.size())/landPolygons.size() > 0.995)
            {
                polygon.water = WaterType.Lake;
                polygonQueue.add(polygon);
                polygonSet.add(polygon);
            }
        }
    }

    private void createOcean() {
        Queue<Polygon> polygonQueue = new ArrayDeque<>();
        Set<Polygon> polygonSet = new HashSet<>();
        generateStartingPointsForOceanGenerator(polygonQueue, polygonSet);
        double oceanCounter = 0;
        while(polygonQueue.size()>0)
        {
            if(1.0 - (oceanCounter / (double)diagram.polygons.size())< settings.getLandmassMinPercentage())
                break;
            Polygon polygon = polygonQueue.poll();
            if(polygon.water == null || polygon.water == WaterType.UnspecifiedWater)
            {
                for(Polygon neighbour: polygon.neighborPolygons)
                {
                    if(!polygonSet.contains(neighbour))
                    {
                        polygonQueue.add(neighbour);
                        polygonSet.add(neighbour);
                    }
                }
                if(polygon.water == null)
                {
                    if(polygon.getWaterToLandNeighbourRatio() > getOceanPolygonRation(diagram.polygons.size())) {polygon.water = WaterType.Ocean;oceanCounter++;}
                    else polygon.water = WaterType.Land;
                }else {polygon.water = WaterType.Ocean; oceanCounter++;}
            }
        }
    }
    private double logOfBase(double num, int base) {
        return Math.log(num) / Math.log(base);
    }
    private double getOceanPolygonRation(int polygonCount) {
        return 0.3 - logOfBase(((double)polygonCount)/50,3) * settings.getWaterLevelConstant();
    }

    private void generateStartingPointsForOceanGenerator(Queue<Polygon> polygonQueue, Set<Polygon> polygonSet) {
        if(settings.isBottomWater() || settings.isLeftWater())
        {
            polygonQueue.add(diagram.pixelPoints[0][diagram.ySize-1].parentPolygon);
            polygonSet.add(diagram.pixelPoints[0][diagram.ySize-1].parentPolygon);
        }
        if(settings.isBottomWater() || settings.isRightWater())
        {
            polygonQueue.add(diagram.pixelPoints[diagram.xSize-1][diagram.ySize-1].parentPolygon);
            polygonSet.add(diagram.pixelPoints[diagram.xSize-1][diagram.ySize-1].parentPolygon);
            polygonQueue.add(diagram.pixelPoints[diagram.xSize-1][diagram.ySize-1].parentPolygon.neighborPolygons.get(0));
            polygonSet.add(diagram.pixelPoints[diagram.xSize-1][diagram.ySize-1].parentPolygon.neighborPolygons.get(0));
        }
        if(settings.isTopWater() || settings.isLeftWater())
        {
            polygonSet.add(diagram.pixelPoints[0][0].parentPolygon);
            polygonQueue.add(diagram.pixelPoints[0][0].parentPolygon);
        }
        if(settings.isTopWater() || settings.isRightWater())
        {
            polygonQueue.add(diagram.pixelPoints[diagram.xSize-1][0].parentPolygon);
            polygonSet.add(diagram.pixelPoints[diagram.xSize-1][0].parentPolygon);
        }

        Random random = new Random();
        if(settings.isBottomWater() && (random.nextInt(3) != 0 || diagram.polygons.size() > 10000))
        {
            polygonQueue.add(diagram.pixelPoints[(int)(diagram.xSize/2)][diagram.ySize-1].parentPolygon);
            polygonSet.add(diagram.pixelPoints[(int)(diagram.xSize/2)][diagram.ySize-1].parentPolygon);
        }
        if(settings.isTopWater() && random.nextInt(4) == 0)
        {
            polygonQueue.add(diagram.pixelPoints[(int)(diagram.xSize/2)][0].parentPolygon);
            polygonSet.add(diagram.pixelPoints[(int)(diagram.xSize/2)][0].parentPolygon);
        }
    }

    private void setMapBordersToWater() {
        for(int x = 0; x < diagram.xSize; x++)
        {
            if(settings.isTopWater())diagram.pixelPoints[x][0].parentPolygon.water = WaterType.UnspecifiedWater;
            if(settings.isBottomWater())diagram.pixelPoints[x][diagram.ySize-1].parentPolygon.water = WaterType.UnspecifiedWater;
        }
        for(int y = 0; y < diagram.ySize; y++)
        {
            if(settings.isLeftWater())diagram.pixelPoints[0][y].parentPolygon.water = WaterType.UnspecifiedWater;
            if(settings.isRightWater())diagram.pixelPoints[diagram.xSize-1][y].parentPolygon.water = WaterType.UnspecifiedWater;
        }
    }
}
