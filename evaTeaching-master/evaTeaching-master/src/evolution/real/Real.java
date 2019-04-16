package evolution.real;

import evolution.*;
import evolution.individuals.Individual;
import evolution.individuals.RealIndividual;
import evolution.operators.*;
import evolution.real.functions.*;
import evolution.selectors.TournamentSelector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Real {

    static int maxGen;
    static int popSize;
    static int dimension;
    static double xoverProb;
    static double mutProb;
    static double mutSigma;
    static String logFilePrefix;
    static String resultsFile;
    static int repeats;
    static Vector<Double> weights;
    static String progFilePrefix;
    static String progFile;
    static String bestPrefix;
    static double eliteSize;
    static double mutProbPerBit;
    static Properties prop;
    static String outputDirectory;
    static String objectiveFilePrefix;
    static String objectiveStatsFile;
    static String fitnessFilePrefix;
    static String fitnessStatsFile;
    static String detailsLogPrefix;
    static int cpu_cores;
    static double discount;
    static double F;
    static double CR;
    static boolean randomF;
    static double temperature;

    public static void main(String[] args) {

        prop = new Properties();
        String propertiesFile = "properties/ga-real.properties";
        try {
            InputStream propIn = new FileInputStream(propertiesFile);
            prop.load(propIn);
        } catch (IOException e) {
            e.printStackTrace();
        }

        maxGen = Integer.parseInt(prop.getProperty("ea.maxGenerations", "20"));
        popSize = Integer.parseInt(prop.getProperty("ea.popSize", "30"));
        xoverProb = Double.parseDouble(prop.getProperty("ea.xoverProb", "0.8"));
        mutProb = Double.parseDouble(prop.getProperty("ea.mutProb", "0.05"));
        mutProbPerBit = Double.parseDouble(prop.getProperty("ea.mutProbPerBit", "0.1"));
        mutSigma = Double.parseDouble(prop.getProperty("ea.mutSigma", "0.04"));
        eliteSize = Double.parseDouble(prop.getProperty("ea.eliteSize", "0.1"));

        dimension = Integer.parseInt(prop.getProperty("prob.dimension", "25"));
        repeats = Integer.parseInt(prop.getProperty("xset.repeats", "10"));
        cpu_cores = Integer.parseInt(prop.getProperty("xset.cpu_cores", "1"));
        discount = Double.parseDouble(prop.getProperty("ea.discount", "0.99"));
        F = Double.parseDouble(prop.getProperty("ea.F"));
        CR = Double.parseDouble(prop.getProperty("ea.CR"));
        randomF = Boolean.parseBoolean(prop.getProperty("ea.randomF"));
        temperature = Double.parseDouble(prop.getProperty("ea.temperature"));


        ArrayList<RealFunction> functions = new ArrayList<RealFunction>();
        functions.add(new F01SphereFunction(dimension));
        functions.add(new F07StepEllipsoidalFunction(dimension));
        functions.add(new F23KatsuuraFunction(dimension));
        functions.add(new F11DiscusFunction(dimension));
        functions.add(new F16WeierstrassFunction(dimension));

        DetailsLogger.disableLog();

        for (RealFunction rf : functions) {

            outputDirectory = prop.getProperty("xlog.outputDirectory", "real") + System.getProperty("file.separator") + rf.getClass().getSimpleName();
            logFilePrefix = prop.getProperty("xlog.filePrefix", "log");
            String path = outputDirectory  + System.getProperty("file.separator") + logFilePrefix;
            objectiveFilePrefix = path + ".objective";
            objectiveStatsFile = path + ".objective_stats";
            bestPrefix = path + ".best";
            fitnessFilePrefix = path + ".fitness";
            fitnessStatsFile = path + ".fitness_stats";
            detailsLogPrefix = path + ".details";

            File outDir = new File(outputDirectory);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            try {
                Files.copy(new File(propertiesFile).toPath(), new File(path + ".properties").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<Individual> bestInds = new ArrayList<Individual>();

            for (int i = 0; i < repeats; i++) {
                RandomNumberGenerator.getInstance().reseed(i);
                rf.reinit();
                Individual best = run(i, rf);
                bestInds.add(best);
            }

            StatsLogger.processResults(fitnessFilePrefix, fitnessStatsFile, repeats, maxGen, popSize);
            StatsLogger.processResults(objectiveFilePrefix, objectiveStatsFile, repeats, maxGen, popSize);

            for (int i = 0; i < bestInds.size(); i++) {
                System.out.println("run " + i + ": best objective=" + bestInds.get(i).getObjectiveValue());
            }

        }

    }

    static Individual run(int number, RealFunction rf) {

        //Initialize logging of the run

        DetailsLogger.startNewLog(detailsLogPrefix + "." + number + ".xml");
        DetailsLogger.logParams(prop);

        try {

            RealIndividual sample = new RealIndividual(dimension, -5.0, 5.0);

            Population pop = new Population();
            pop.setPopulationSize(popSize);
            pop.setSampleIndividual(sample);
            pop.createRandomInitialPopulation();

            EvolutionaryAlgorithm ea = new EvolutionaryAlgorithm();
            ea.setCPUCores(cpu_cores);
            ea.addMatingSelector(new MySelector());
            ea.addOperator(new AveragingCrossoverOperator(xoverProb));
            //ea.addOperator(new MyMutationOperator(mutProb, mutProbPerBit, mutSigma, discount));
            //ea.addOperator(new PolynomialMutationOperator(mutProb,mutProbPerBit));
            //ea.addOperator(new DifferentialMutation(mutProb,F, dimension, CR, mutProbPerBit,new RealFitnessFunction(rf), randomF));
            ea.addOperator(new SimulatedAnnealingMutation(temperature,mutSigma,mutProb,mutProbPerBit,new RealFitnessFunction(rf)));
            ea.setFitnessFunction(new RealFitnessFunction(rf));
            ea.addEnvironmentalSelector(new TournamentSelector());
            ea.setElite(eliteSize);

            OutputStreamWriter fitnessOut = new OutputStreamWriter(new FileOutputStream(fitnessFilePrefix + "." + number));
            OutputStreamWriter objectiveOut = new OutputStreamWriter(new FileOutputStream(objectiveFilePrefix + "." + number));

            for (int i = 0; i < maxGen; i++) {
                ea.evolve(pop);
                if (i % 100 == 0) {
                    RealIndividual ri = (RealIndividual)pop.getSortedIndividuals().get(0);
                    Double diff = ri.getObjectiveValue();
                    System.out.println("Generation " + i + ": "  + diff + " | " + printArray(ri.toDoubleArray()));
                    System.out.println("\tGradient: " + printArray(rf.numericalDerivative(ri.toDoubleArray())));
                }
                StatsLogger.logFitness(pop, fitnessOut);
                StatsLogger.logObjective(pop, objectiveOut);
            }

            RealIndividual ri = (RealIndividual)pop.getSortedIndividuals().get(0);
            Double diff = ri.getObjectiveValue();
            System.out.println("End: " + diff + " | " + printArray(ri.toDoubleArray()));
            System.out.println("\tGradient: " + printArray(rf.numericalDerivative(ri.toDoubleArray())));

            OutputStreamWriter bestOut = new OutputStreamWriter(new FileOutputStream(bestPrefix + "." + number));

            bestOut.write("Fitness: " + ri.getFitnessValue() + "\n");
            bestOut.write("Objective: " + ri.getObjectiveValue() + "\n");
            bestOut.write("Individual: " + printArray(ri.toDoubleArray()) + "\n");
            bestOut.write("Gradient: " + printArray(rf.numericalDerivative(ri.toDoubleArray())) + "\n");
            bestOut.write("Xopt: " + printArray(rf.getXopt()) + "\n");
            bestOut.write("Fopt: " + rf.getFopt() + "\n");
            bestOut.write("F(Xopt): " + (-rf.evaluate(rf.getXopt())) + "\n");

            fitnessOut.close();
            objectiveOut.close();
            bestOut.close();

            DetailsLogger.writeLog();

            return ri;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    static String printArray(double[] a) {
        StringBuilder sb = new StringBuilder();

        for (double d: a) {
            sb.append(String.format(Locale.US, "%.5f ", d));
        }

        return sb.toString();
    }
}
