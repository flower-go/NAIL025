package evolution.real.functions;

import evolution.RandomNumberGenerator;

/**
 * Created with IntelliJ IDEA.
 * User: Martin Pilat
 * Date: 11.11.13
 * Time: 19:42
 * To change this template use File | Settings | File Templates.
 */
public class F03RastriginFunction extends RealFunction {

    public F03RastriginFunction(int D) {
        super(D);
    }

    @Override
    public void reinit() {
        initXopt();
        double g1 = RandomNumberGenerator.getInstance().nextGaussian();
        double g2 = RandomNumberGenerator.getInstance().nextGaussian();
        fopt = Math.min(1000.0, Math.max(-1000.0, (Math.round(100.0 * 100.0 * g1 / g2) / 100.0)));
        lambda = createLambda(10);
    }

    @Override
    public double value(double[] x) {
        double[] z = diagMult(lambda, Tasy(Tosz(minus(x, xopt)), 0.2));

        double sum = 0;
        for (double d : z) {
            sum += Math.cos(2 * Math.PI * d);
        }

        return 10 * (D - sum) + norm2(z) + fopt;
    }

}
