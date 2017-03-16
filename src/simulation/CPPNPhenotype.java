package simulation;

import neat.ConnectionGene;
import neat.Genotype;
import neat.NodeGene;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by colander on 1/3/17.
 * Class used as the single phenotype constructed from a genotype.
 */
public class CPPNPhenotype {

    public HashMap<Integer, NetworkNode> nodesByInnov = new HashMap<>();
    ArrayList<NetworkNode> network = new ArrayList<>();
    ArrayList<NetworkNode> inputs = new ArrayList<>();
    ArrayList<NetworkNode> outputs = new ArrayList<>();
    ArrayList<NetworkNode> hidden = new ArrayList<>();

    public CPPNPhenotype(Genotype g) {
        for (int i = 0; i < g.nodeGenes.size(); i++) {
            int nodeInnov = g.nodeGenes.get(i).innov;
            NetworkNode node = new NetworkNode(nodeInnov, g.nodeGenes.get(i).activateFunction);
            nodesByInnov.put(nodeInnov, node);
            network.add(node);
            if (g.nodeGenes.get(i).type == NodeGene.TYPE_INPUT) inputs.add(node);
            else if (g.nodeGenes.get(i).type == NodeGene.TYPE_OUTPUT) outputs.add(node);
            else hidden.add(node);
        }

        for (int i = 0; i < g.connectionGenes.size(); i++) {
            ConnectionGene connectionGene = g.connectionGenes.get(i);
            if (!connectionGene.active) continue;
            nodesByInnov.get(connectionGene.out).inputs.add(nodesByInnov.get(connectionGene.in));
            nodesByInnov.get(connectionGene.out).inputWeights.add(connectionGene.weight);
        }
        this.network.forEach(node -> node.triggered = true);
    }

    double[] step(double[] inputs) {
        hidden.forEach(node -> node.triggered = false);
        for (int i = 0; i < this.inputs.size(); i++) {
            this.inputs.get(i).currentValue = inputs[i];
        }

        double[] out = new double[outputs.size()];
        for (int i = 0; i < outputs.size(); i++) {
            triggerNode(outputs.get(i));
            out[i] = outputs.get(i).currentValue;
        }
        return out;
    }

    private void triggerNode(NetworkNode node) {
        node.inputs.stream().filter(input -> !input.triggered).forEach(this::triggerNode);
        double sum = 0;
        for (int i = 0; i < node.inputs.size(); i++) {
            sum += node.inputs.get(i).currentValue * node.inputWeights.get(i);
        }

        switch (node.activationFunction) {
            case NodeGene.FUNCTION_SIGMOID:
                node.currentValue = ActivationFunctions.sigmoid(sum);
                break;
            case NodeGene.FUNCTION_SIN:
                node.currentValue = ActivationFunctions.sin(sum);
                break;
            case NodeGene.FUNCTION_COS:
                node.currentValue = ActivationFunctions.cos(sum);
                break;
            case NodeGene.FUNCTION_LINEAR:
                node.currentValue = ActivationFunctions.linear(sum);
                break;
            case NodeGene.FUNCTION_ABS:
                node.currentValue = ActivationFunctions.abs(sum);
                break;

            default:
                System.err.println("WRONG ACTIVATION FUNCTION VALUE: " + node.activationFunction + ", node innov: " + node.innov);
                System.exit(1);
                break;
        }
        node.triggered = true;
    }
}