package evolution.real.functions;

import evolution.RandomNumberGenerator;

/**
 * Created with IntelliJ IDEA.
 * User: Martin Pilat
 * Date: 11.11.13
 * Time: 19:42
 * To change this template use File | Settings | File Templates.
 */
public class F13SharpRidgeFunction extends RealFunction {

    public F13SharpRidgeFunction(int D) {
        super(D);
    }

    @Override
    public void reinit() {
        initXopt();
        double g1 = RandomNumberGenerator.getInstance().nextGaussian();
        double g2 = RandomNumberGenerator.getInstance().nextGaussian();
        fopt = Math.min(1000.0, Math.max(-1000.0, (Math.round(100.0 * 100.0 * g1 / g2) / 100.0)));
        R = getRandomRotationMatrix();
        Q = getRandomRotationMatrix();
        lambda = createLambda(10);
    }

    @Override
    public double value(double[] x) {
        double[] z = mult(Q, diagMult(lambda, mult(R, minus(x, xopt))));

        double sum = 0.0;
        for (int i = 1; i < D; i++) {
            sum += z[i] * z[i];
        }

        return z[1] * z[1] + 100 * Math.sqrt(sum) + fopt;
    }

}
