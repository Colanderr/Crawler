package com.janboucek.crawler.simulation;

import com.janboucek.crawler.neat.Genotype;
import com.janboucek.crawler.worldbuilding.BodySettings;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

/**
 * Created by colander on 1/13/17.
 * Class used to measure the fitness of a single genotype.
 */
public class FitnessTest implements Comparable<FitnessTest> {
    private static final double HEIGHT_LIMIT = -13.0;
    private final int ITERATIONS = 3000;
    private final int CONFIRM_ITERATIONS = 1500;
    final private boolean LIMIT_HEIGHT = true;

    private World world;
    public Genotype genotype;
    private FitnessSimulationStepper stepper;
    private int id;

    double result;

    FitnessTest(Genotype g, BodySettings bodySettings, int id) {
        this.id = id;
        this.genotype = g;
        this.world = new World(new Vec2(0f, 0f)); //setting the gravity is a responsibility of the WorldBuilder
        stepper = new FitnessSimulationStepper(world, bodySettings, g);
    }

    FitnessTest compute() {
        final boolean[] failed = {false};
        float maxX = 0f;

        for (int i = 0; i < ITERATIONS + (LIMIT_HEIGHT ? CONFIRM_ITERATIONS : 0); i++) {
            stepper.step(true);
            if (stepper.robot.body.getPosition().x > maxX && i < ITERATIONS)
                maxX = stepper.robot.body.getPosition().x;
            if (LIMIT_HEIGHT && stepper.robot.legs.stream().anyMatch(leg -> leg.segments.stream().anyMatch(segment -> segment.getPosition().y < HEIGHT_LIMIT))) {
                failed[0] = true;
                break;
            }
        }

        result = failed[0] ? 0 : maxX;
        result = Math.max(result, 0.000001);
        //free up memory ASAP
        world = null;
        System.gc();

        return this;
    }

    @Override
    public int compareTo(FitnessTest o) {
        return Double.compare(this.result, o.result) == 0 ? Integer.compare(id, o.id) : Double.compare(this.result, o.result);
    }
}