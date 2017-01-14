package neat;

import javafx.util.Pair;
import org.jbox2d.testbed.framework.TestbedController;
import org.jbox2d.testbed.framework.TestbedFrame;
import org.jbox2d.testbed.framework.TestbedModel;
import org.jbox2d.testbed.framework.TestbedPanel;
import org.jbox2d.testbed.framework.j2d.TestPanelJ2D;
import simulation.FitnessTest;
import simulation.TestbedFitnessTest;
import worldbuilding.BodySettings;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by colander on 1/3/17.
 */
public class Evolution {
    //final int GENERATIONS = 10; TODO remove?
    final int GENERATION_SIZE = 100;
    final double DEFAULT_WEIGHT_RANGE = 2;

    //mutation chances
    final double MUTATE_ADD_NODE = 0.05;
    final double MUTATE_ADD_CONNECTION = 0.05;
    final double MUTATE_WEIGHT_SMALL = 0.05;
    final double MUTATE_WEIGHT_RANDOM = 0.05;

    final double MUTATE_SMALL_LIMIT = 0.05;

    final double COMPAT_1 = 1.0;
    final double COMPAT_2 = 0.4;
    final double DELTA_T = 3.0;

    final double CROSSOVER = 0.75;
    final double KILL_OFF = 0.5;

    final Random random = new Random(1337 * 420);
    final int INPUT_NODES;
    final int OUTPUT_NODES;

    private BodySettings bodySettings;

    int innovation = 0;
    private ArrayList<Genotype> generation = new ArrayList<>();
    private ArrayList<Species> species = new ArrayList<>();

    double best = 0;

    //TODO maybe add variable bodySettings
    public Evolution(BodySettings bodySettings) {
        this.bodySettings = bodySettings;
        INPUT_NODES = bodySettings.legs * bodySettings.segments;
        OUTPUT_NODES = bodySettings.legs * bodySettings.segments;

        for (int i = 0; i < GENERATION_SIZE; i++) {
            generation.add(new Genotype(new ArrayList<>(), new ArrayList<>()));
        }
        for (int j = 0; j < INPUT_NODES; j++) {
            for (int i = 0; i < GENERATION_SIZE; i++) {
                generation.get(i).nodeGenes.add(new NodeGene(innovation, NodeGene.TYPE_INPUT));
            }
            innovation++;
        }
        for (int j = 0; j < OUTPUT_NODES; j++) {
            for (int i = 0; i < GENERATION_SIZE; i++) {
                generation.get(i).nodeGenes.add(new NodeGene(innovation, NodeGene.TYPE_OUTPUT));
            }
            innovation++;
        }


        //TESTING
        /*for (int i = 0; i < 100; i++) {
            mutateAddConnection(generation.get(0));
            mutateAddConnection(generation.get(1));
        }
        for (int i = 0; i < 10; i++) {
            mutateSplitConnection(generation.get(0));
            mutateSplitConnection(generation.get(1));
        }

        Util.printGenotype(generation.get(0));
        Genotype cross = crossOver(generation.get(0), generation.get(1));

        FitnessTest fit = new FitnessTest(generation.get(0), bodySettings);
        System.out.println(fit.compute());
        /*for (int i = 0; i < 10; i++) {
            mutateEnableDisableConnection(generation.get(0));
        }*/

    }

    public ArrayList<Genotype> nextGeneration() {
        //TODO implement

        //MEASURE FITNESSES
        ArrayList<FitnessTest> fitnesses = new ArrayList<>();
        for (int i = 0; i < generation.size(); i++) {
            FitnessTest test = new FitnessTest(generation.get(i), bodySettings);
            test.compute();
            fitnesses.add(test);
        }
        Collections.sort(fitnesses);

        if (fitnesses.get(fitnesses.size() - 1).result > best) {
            best = fitnesses.get(fitnesses.size() - 1).result;
            TestbedModel model = new TestbedModel();
            model.addTest(new TestbedFitnessTest(fitnesses.get(fitnesses.size() - 1).genotype, bodySettings));
            TestbedPanel panel = new TestPanelJ2D(model);
            TestbedFrame frame = new TestbedFrame(model, panel, TestbedController.UpdateBehavior.UPDATE_CALLED);
            frame.setVisible(true);
        }

        //SPECIATION
        for (int i = 0; i < fitnesses.size(); i++) {
            boolean found = false;
            for (int j = 0; j < species.size(); j++) {
                if (distance(fitnesses.get(i).genotype, species.get(j).archetype) < DELTA_T) {
                    found = true;
                    species.get(j).genotypes.add(new Pair<>(fitnesses.get(i).genotype, fitnesses.get(i).result));
                    break;
                }
            }
            if (!found) {
                Species nspecies = new Species(fitnesses.get(i).genotype);
                nspecies.genotypes.add(new Pair<>(fitnesses.get(i).genotype, fitnesses.get(i).result));
                species.add(nspecies);
            }
        }


        System.out.println(species.size());

        double sum = 0;
        for (int i = 0; i < species.size(); i++) {
            Species spec = species.get(i);
            double curSum = 0;
            for (int j = 0; j < spec.genotypes.size(); j++) {
                curSum += spec.genotypes.get(j).getValue();
            }
            spec.avgFitness = curSum / spec.genotypes.size();
            sum += spec.avgFitness;
        }

        //BREEDING
        ArrayList<Genotype> children = new ArrayList<>();

        for (int i = 0; i < species.size(); i++) {
            //kill off the weak members
            int targetSize = Math.max((int) (species.get(i).genotypes.size() * KILL_OFF), 1);
            while (species.get(i).genotypes.size() > targetSize) {
                species.get(i).genotypes.remove(0);
            }

            int toBreed = (int) (species.get(i).avgFitness / sum * GENERATION_SIZE);
            for (int j = 0; j < toBreed; j++) {
                if (random.nextDouble() < CROSSOVER) {
                    children.add(Util.copyGenotype(species.get(i).genotypes.get(random.nextInt(species.get(i).genotypes.size())).getKey()));
                } else {
                    Genotype a = species.get(i).genotypes.get(random.nextInt(species.get(i).genotypes.size())).getKey();
                    Genotype b = species.get(i).genotypes.get(random.nextInt(species.get(i).genotypes.size())).getKey();
                    children.add(crossOver(a, b));
                }
            }
        }

        //MUTATE
        for (int i = 0; i < children.size(); i++) {
            if (random.nextDouble() < MUTATE_ADD_CONNECTION) mutateAddConnection(children.get(i));
            if (random.nextDouble() < MUTATE_ADD_NODE && !children.get(i).connectionGenes.isEmpty())
                mutateSplitConnection(children.get(i));
            if (random.nextDouble() < MUTATE_WEIGHT_SMALL && !children.get(i).connectionGenes.isEmpty())
                mutateWightSmall(children.get(i));
            if (random.nextDouble() < MUTATE_WEIGHT_RANDOM && !children.get(i).connectionGenes.isEmpty())
                mutateWeightRandom(children.get(i));
        }

        generation = children;

        //CLEANUP
        //clear species, new archetypes
        for (int i = 0; i < species.size(); i++) {
            species.get(i).archetype = species.get(i).genotypes.get(species.get(i).genotypes.size() - 1).getKey();
            species.get(i).genotypes.clear();

        }

        //LOG RESULTS
        System.out.println("MAX FITNESS: " + fitnesses.get(fitnesses.size() - 1).result + " SUM: " + sum + " SPEC: " + species.size() + " GEN: " + generation.size());
        return generation;
    }

    private void mutateAddConnection(Genotype g) {
        //retrieves the list of all non-edges to choose from
        ArrayList<Pair<Integer, Integer>> nonEdgeList = Util.getNonEdgeList(g);
        //remove all non-edges leading to an input
        Iterator<Pair<Integer, Integer>> it = nonEdgeList.iterator();
        while (it.hasNext()) {
            Pair<Integer, Integer> cur = it.next();
            if (cur.getValue() < INPUT_NODES || cur.getKey() < INPUT_NODES + OUTPUT_NODES && cur.getKey() >= INPUT_NODES)
                it.remove();

        }
        if (nonEdgeList.size() == 0) return;
        Pair<Integer, Integer> coord = nonEdgeList.get(random.nextInt(nonEdgeList.size()));
        g.connectionGenes.add(new ConnectionGene(coord.getKey(), coord.getValue(), random.nextDouble() * 2 * DEFAULT_WEIGHT_RANGE - DEFAULT_WEIGHT_RANGE, true, ++innovation));
    }

    private void mutateSplitConnection(Genotype g) {
        ConnectionGene toSplit = g.connectionGenes.get(random.nextInt(g.connectionGenes.size()));
        int nodeInnov = ++innovation;
        g.nodeGenes.add(new NodeGene(nodeInnov, NodeGene.TYPE_HIDDEN));
        g.connectionGenes.add(new ConnectionGene(toSplit.in, nodeInnov, 1.0, true, ++innovation));
        g.connectionGenes.add(new ConnectionGene(nodeInnov, toSplit.out, toSplit.weight, true, ++innovation));
        toSplit.active = false;
    }

    private void mutateEnableDisableConnection(Genotype g) {
        ConnectionGene gene = g.connectionGenes.get(random.nextInt(g.connectionGenes.size()));
        gene.active = !gene.active;
    }

    private void mutateWightSmall(Genotype g) {
        g.connectionGenes.get(random.nextInt(g.connectionGenes.size())).weight *= 1 + random.nextDouble() * (random.nextBoolean() ? MUTATE_SMALL_LIMIT : -MUTATE_SMALL_LIMIT);
    }

    private void mutateWeightRandom(Genotype g) {
        g.connectionGenes.get(random.nextInt(g.connectionGenes.size())).weight = random.nextDouble() * 2 * DEFAULT_WEIGHT_RANGE - DEFAULT_WEIGHT_RANGE;
    }

    //genotype a is the fitter one (decides disjoint and excess genes)
    public Genotype crossOver(Genotype a, Genotype b) {
        //NODES
        /* unnecessary code, nodegenes do not mutate
        ArrayList<Pair<NodeGene, NodeGene>> commonNodes = new ArrayList<>();
        ArrayList<NodeGene> dominantNodes = new ArrayList<>();
        for (int i = 0; i < a.nodeGenes.size(); i++) {
            boolean found = false;
            for (int j = 0; j < b.nodeGenes.size(); j++) {
                if (a.nodeGenes.get(i).innov == b.nodeGenes.get(j).innov) {
                    commonNodes.add(new Pair<>(a.nodeGenes.get(i), a.nodeGenes.get(j)));
                    found = true;
                    break;
                }
            }
            if (!found) dominantNodes.add(a.nodeGenes.get(i));
        }
        ArrayList<NodeGene> nodeGenes = new ArrayList<>();
        for (int i = 0; i < commonNodes.size(); i++) {
            if (random.nextBoolean()) {
                nodeGenes.add(commonNodes.get(i).getKey());
            } else {
                nodeGenes.add(commonNodes.get(i).getValue());
            }
        }
        nodeGenes.addAll(dominantNodes);*/

        //CONNECTIONS
        ArrayList<Pair<ConnectionGene, ConnectionGene>> commonConnections = new ArrayList<>();
        ArrayList<ConnectionGene> dominantConnections = new ArrayList<>();
        for (int i = 0; i < a.connectionGenes.size(); i++) {
            boolean found = false;
            for (int j = 0; j < b.connectionGenes.size(); j++) {
                if (a.connectionGenes.get(i).innovation == b.connectionGenes.get(j).innovation) {
                    commonConnections.add(new Pair<>(a.connectionGenes.get(i), a.connectionGenes.get(j)));
                    found = true;
                    break;
                }
            }
            if (!found) dominantConnections.add(a.connectionGenes.get(i));
        }
        ArrayList<ConnectionGene> connectionGenes = new ArrayList<>();
        for (int i = 0; i < commonConnections.size(); i++) {
            if (random.nextBoolean()) {
                connectionGenes.add(commonConnections.get(i).getKey());
            } else {
                connectionGenes.add(commonConnections.get(i).getValue());
            }
        }
        connectionGenes.addAll(dominantConnections);

        ArrayList<ConnectionGene> cpConnections = connectionGenes.stream().map(Util::copyConnection).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<NodeGene> cpNodes = a.nodeGenes.stream().map(Util::copyNode).collect(Collectors.toCollection(ArrayList::new)); //!!!

        return new Genotype(cpNodes, cpConnections);
    }

    public double distance(Genotype a, Genotype b) {
        double W = 0;
        int common = 0;
        HashMap<Integer, ConnectionGene> map = new HashMap<>();
        for (int i = 0; i < a.connectionGenes.size(); i++) {
            map.put(a.connectionGenes.get(i).innovation, a.connectionGenes.get(i));
        }
        for (int i = 0; i < b.connectionGenes.size(); i++) {
            ConnectionGene mutual = map.get(b.connectionGenes.get(i).innovation);
            if (mutual != null) {
                W += Math.abs(mutual.weight - b.connectionGenes.get(i).weight);
                common++;
            }
        }
        W /= common;
        if (common == 0) W = 0;
        int D = a.connectionGenes.size() + b.connectionGenes.size() - 2 * common;
        int N = Math.max(1, Math.max(a.connectionGenes.size(), b.connectionGenes.size()));
        return (COMPAT_1 / N * D) + (COMPAT_2 * W);
    }
}
