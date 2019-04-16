package evolution.multi;

import evolution.*;
import evolution.individuals.Individual;
import evolution.individuals.MultiRealIndividual;
import evolution.individuals.RealIndividual;
import evolution.multi.functions.*;
import evolution.operators.AveragingCrossoverOperator;
import evolution.operators.GaussianMutationOperator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Multi {

    static int maxGen;
    static int popSize;
    static int dimension;
    static double xoverProb;
    static double mutProb;
    static double mutProbPerBit;
    static double mutSigma;
    static String logFilePrefix;
    static int repeats;
    static String bestPrefix;
    static Properties prop;
    static String outputDirectory;
    static String objectiveFilePrefix;
    static String objectiveStatsFile;
    static String fitnessFilePrefix;
    static String fitnessStatsFile;
    static String detailsLogPrefix;
    static int cpu_cores;

    public static void main(String[] args) {

        String propertiesFile = "properties/ga-multi.properties";
        prop = new Properties();
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
        mutProbPerBit = Double.parseDouble(prop.getProperty("ea.mutProbPerBit", "1.0"));
        mutSigma = Double.parseDouble(prop.getProperty("ea.mutSigma", "0.04"));
        cpu_cores = Integer.parseInt(prop.getProperty("xset.cpu_cores", "1"));

        dimension = Integer.parseInt(prop.getProperty("prob.dimension", "25"));
        repeats = Integer.parseInt(prop.getProperty("xset.repeats", "10"));

        DetailsLogger.disableLog();

        ArrayList<MultiObjectiveFunction> mofs = new ArrayList<MultiObjectiveFunction>();
        mofs.add(new ZDT1());
        /*mofs.add(new ZDT2());
        mofs.add(new ZDT3());
        mofs.add(new ZDT4());
        mofs.add(new ZDT6());*/


        for (MultiObjectiveFunction mof : mofs) {

            outputDirectory = prop.getProperty("xlog.outputDirectory", "multi") + System.getProperty("file.separator") + mof.getClass().getSimpleName();
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

            List<Double> hypervols = new ArrayList<Double>();

            for (int i = 0; i < repeats; i++) {
                RandomNumberGenerator.getInstance().reseed(i);
                Double hv = run(i, mof);
                hypervols.add(hv);
            }

            StatsLogger.processResults(fitnessFilePrefix, fitnessStatsFile, repeats, maxGen, popSize);
            StatsLogger.processResults(objectiveFilePrefix, objectiveStatsFile, repeats, maxGen, popSize);

            for (int i = 0; i < hypervols.size(); i++) {
                System.out.println("run " + i + ": hypervolumeDifference=" + hypervols.get(i));
            }
        }
    }

    static double run(int number, MultiObjectiveFunction mof) {

        try {

            RealIndividual sample = new MultiRealIndividual(dimension, 0.0, 1.0);

            Population pop = new Population();
            pop.setPopulationSize(popSize);
            pop.setSampleIndividual(sample);
            pop.createRandomInitialPopulation();

            EvolutionaryAlgorithm ea = new EvolutionaryAlgorithm();
            ea.setCPUCores(cpu_cores);
            ea.addOperator(new AveragingCrossoverOperator(xoverProb));
            ea.addOperator(new GaussianMutationOperator(mutProb, mutProbPerBit, mutSigma));
            ea.setFitnessFunction(new MultiObjectiveFitnessFunction(mof));
            ea.addEnvironmentalSelector(new NSGA2Selector());
            ea.setReplacement(new MergingReplacement());

            OutputStreamWriter fitnessOut = new OutputStreamWriter(new FileOutputStream(fitnessFilePrefix + "." + number));
            OutputStreamWriter objectiveOut = new OutputStreamWriter(new FileOutputStream(objectiveFilePrefix + "." + number));

            for (int i = 0; i < maxGen; i++) {
                ea.evolve(pop);
                MultiRealIndividual mri = (MultiRealIndividual)pop.getSortedIndividuals().get(0);
                double hypervolume = mof.getOptimalHypervolume() - MultiObjectiveUtils.calculateHypervolume(pop, mof.getReferencePoint());
                mri.setObjectiveValue(hypervolume);
                if (i % 100 == 0) {
                    System.out.println("Generation " + i + ": "  + hypervolume);
                    }
                StatsLogger.logFitness(pop, fitnessOut);
                StatsLogger.logObjective(pop, objectiveOut);
            }

            double hypervolume = mof.getOptimalHypervolume() - MultiObjectiveUtils.calculateHypervolume(pop, mof.getReferencePoint());
            System.out.println("End: "  + hypervolume);

            OutputStreamWriter bestOut = new OutputStreamWriter(new FileOutputStream(bestPrefix + "." + number));

            for (int i = 0; i < pop.getPopulationSize(); i++) {
                MultiObjectiveUtils.printIndividual((MultiRealIndividual)pop.get(i), bestOut);
            }

            fitnessOut.close();
            objectiveOut.close();
            bestOut.close();

            return hypervolume;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Double.NaN;
    }
}
