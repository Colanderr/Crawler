package simulation;

import neat.Genotype;
import org.jbox2d.testbed.framework.TestbedSettings;
import org.jbox2d.testbed.framework.TestbedTest;
import worldbuilding.BodySettings;

/**
 * Created by colander on 1/13/17.
 * Class used to show a phenotype fitness test in a GUI.
 */
public class TestbedFitnessTest extends TestbedTest {

    private int frames = 0;

    private BodySettings bodySettings;
    private Genotype g;

    private FitnessSimulationStepper stepper;

    private double target;
    private double maxX = 0;


    public TestbedFitnessTest(Genotype g, BodySettings bodySettings, double target) {
        super();
        this.setTitle("Fitness");
        this.target = target;
        this.g = g;
        this.bodySettings = bodySettings;
    }

    @Override
    public void initTest(boolean b) {
        stepper = new FitnessSimulationStepper(getWorld(), bodySettings, g);
    }

    @Override
    public String getTestName() {
        return "Fitness";
    }

    @Override
    public synchronized void step(TestbedSettings settings) {
        frames++;
        stepper.step(false);
        float curx = stepper.robot.body.getPosition().x;
        if (curx > maxX) maxX = curx;
        this.addTextLine("X: " + curx);
        this.addTextLine("M: " + maxX);
        this.addTextLine("T: " + target);
        this.addTextLine("FRAMES: " + frames);
        super.step(settings);
    }

    public void update() {
        super.update();
    }
}
