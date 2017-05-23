package simulation;

import neat.Genotype;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import testsettings.TestSettings;
import worldbuilding.BodySettings;
import worldbuilding.WorldBuilder;
import worldbuilding.WorldSettings;

/**
 * Created by colander on 1/18/17.
 * Class used to commit the actual steps (frames) of the simulation.
 */
public class FitnessSimulationStepper {
    private static final double FUNC_DIVIDER = 40.0;
    private final float TIME_STEP = 1 / 60f;
    private final int VEL_ITERATIONS = 8;
    private final int POS_ITERATIONS = 3;
    private final float SPEED_MULTIPLIER = 6.5f;
    private final int STARTUP_FRAMES = 30; //frames at the start when the robot is falling and is not allowed to move
    private final double TOUCH_CHANGE_SPEED = 0.08;

    private int framesElapsed = 0;

    private World world;
    Robot robot;
    public Genotype genotype;
    public ANNPhenotype annPhenotype;

    FitnessSimulationStepper(World world, BodySettings bodySettings, Genotype g) {
        WorldSettings worldSettings = new WorldSettings(10.0f);
        WorldBuilder worldBuilder = new WorldBuilder(world, bodySettings, worldSettings);
        robot = worldBuilder.build();
        this.world = world;
        CPPNPhenotype cppnPhenotype = new CPPNPhenotype(g);
        this.annPhenotype = new ANNPhenotype(cppnPhenotype);

        this.world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                for (RobotLeg leg : robot.legs) {
                    if (contact.getFixtureA().getBody() == leg.segments.get(1) || contact.getFixtureB().getBody() == leg.segments.get(1)) {
                        leg.touch = true;
                    }
                }
            }

            @Override
            public void endContact(Contact contact) {
                for (RobotLeg leg : robot.legs) {
                    if (contact.getFixtureA().getBody() == leg.segments.get(1) || contact.getFixtureB().getBody() == leg.segments.get(1)) {
                        leg.touch = false;
                    }
                }
            }

            @Override
            public void preSolve(Contact contact, Manifold manifold) {

            }

            @Override
            public void postSolve(Contact contact, ContactImpulse contactImpulse) {

            }
        });
    }


    void step(boolean stepWorld) {
        if (++framesElapsed > STARTUP_FRAMES) {
            robot.legs.forEach(l -> l.touchValue = l.touch ? Math.min(1, l.touchValue + TOUCH_CHANGE_SPEED) : Math.max(-1, l.touchValue - TOUCH_CHANGE_SPEED));
            double[][] inputs = {
                    //first row - leg 0 and 1
                    {
                            robot.legs.get(0).touchValue,
                            angleToValue0(robot.legs.get(0).joints.get(0).getJointAngle(), true),
                            angleToValue1(robot.legs.get(0).joints.get(1).getJointAngle(), true),
                            angleToValue1(robot.legs.get(1).joints.get(1).getJointAngle(), true),
                            angleToValue0(robot.legs.get(1).joints.get(0).getJointAngle(), true),
                            robot.legs.get(1).touchValue
                    },
                    //second row - extras
                    {
                            robot.body.getAngle(),
                            1,
                            Math.sin(framesElapsed / FUNC_DIVIDER),
                            Math.cos(framesElapsed / FUNC_DIVIDER),
                            1,
                            robot.body.getAngle()
                    },
                    //third row - leg 2 and 3
                    {
                            robot.legs.get(2).touchValue,
                            angleToValue0(robot.legs.get(2).joints.get(0).getJointAngle(), false),
                            angleToValue1(robot.legs.get(2).joints.get(1).getJointAngle(), false),
                            angleToValue1(robot.legs.get(3).joints.get(1).getJointAngle(), false),
                            angleToValue0(robot.legs.get(3).joints.get(0).getJointAngle(), false),
                            robot.legs.get(3).touchValue
                    }
            };
            double[][] outputs = this.annPhenotype.step(inputs);

            setAngle0(robot.legs.get(0).joints.get(0), outputs[0][1], true);
            setAngle1(robot.legs.get(0).joints.get(1), outputs[0][2], true);
            setAngle1(robot.legs.get(1).joints.get(1), outputs[0][3], true);
            setAngle0(robot.legs.get(1).joints.get(0), outputs[0][4], true);
            setAngle0(robot.legs.get(2).joints.get(0), outputs[2][1], false);
            setAngle1(robot.legs.get(2).joints.get(1), outputs[2][2], false);
            setAngle1(robot.legs.get(3).joints.get(1), outputs[2][3], false);
            setAngle0(robot.legs.get(3).joints.get(0), outputs[2][4], false);
        }
        if (stepWorld) world.step(TIME_STEP, VEL_ITERATIONS, POS_ITERATIONS);
    }

    private void setAngle0(RevoluteJoint joint, double value, boolean left) {
        //if (1 == 1) return;
        //System.out.println((float) (valueToAngle0(value, left) - joint.getJointAngle()) * SPEED_MULTIPLIER);
        joint.setMotorSpeed((float) (valueToAngle0(value, left) - joint.getJointAngle()) * SPEED_MULTIPLIER);
    }

    private void setAngle1(RevoluteJoint joint, double value, boolean left) {
        //if (1 == 1) return;
        joint.setMotorSpeed((float) (valueToAngle1(value, left) - joint.getJointAngle()) * SPEED_MULTIPLIER);
    }

    private double angleToValue0(double angle, boolean left) {
        return 1 - angle / (Math.PI * 3 / 4.0 * (left ? -1 : 1));
    }

    private double angleToValue1(double angle, boolean left) {
        return (angle + (Math.PI * 5 / 2.0 * (!left ? 1 : -1))) / (Math.PI / 2);
    }

    private double valueToAngle0(double value, boolean left) {
        if (value < 0) return 0;
        return Math.abs((1 - value) * (Math.PI * 3 / 4.0 * (left ? -1 : 1)));
    }

    private double valueToAngle1(double value, boolean left) {
        if (value < 0) return 0;
        return Math.abs(value * (Math.PI / 2) - (Math.PI * 5 / 2.0 * (!left ? 1 : -1)));
    }
}
