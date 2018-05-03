package com.arboratum.beangen.util;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.core.EnumeratedDistributionGeneratorBuilder;
import org.apache.commons.math3.exception.TooManyIterationsException;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

import static com.arboratum.beangen.BaseBuilders.enumerated;

/**
 * for each Bin p0 = sum(proba of combination | where bin is 0)
 *
 * @author gpicron.
 */
public class BinomialCombGeneratorBuilder {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BinomialCombGeneratorBuilder.class);

    @java.beans.ConstructorProperties({"combinationName"})
    public BinomialCombGeneratorBuilder(String combinationName) {
        this.combinationName = combinationName;
    }

    public String getCombinationName() {
        return this.combinationName;
    }

    public List<Binomial> getBinomials() {
        return this.binomials;
    }

    public Set<Case> getExclusions() {
        return this.exclusions;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BinomialCombGeneratorBuilder)) return false;
        final BinomialCombGeneratorBuilder other = (BinomialCombGeneratorBuilder) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$combinationName = this.getCombinationName();
        final Object other$combinationName = other.getCombinationName();
        if (this$combinationName == null ? other$combinationName != null : !this$combinationName.equals(other$combinationName))
            return false;
        final Object this$binomials = this.getBinomials();
        final Object other$binomials = other.getBinomials();
        if (this$binomials == null ? other$binomials != null : !this$binomials.equals(other$binomials)) return false;
        final Object this$exclusions = this.getExclusions();
        final Object other$exclusions = other.getExclusions();
        if (this$exclusions == null ? other$exclusions != null : !this$exclusions.equals(other$exclusions))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $combinationName = this.getCombinationName();
        result = result * PRIME + ($combinationName == null ? 43 : $combinationName.hashCode());
        final Object $binomials = this.getBinomials();
        result = result * PRIME + ($binomials == null ? 43 : $binomials.hashCode());
        final Object $exclusions = this.getExclusions();
        result = result * PRIME + ($exclusions == null ? 43 : $exclusions.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof BinomialCombGeneratorBuilder;
    }

    public String toString() {
        return "BinomialCombGeneratorBuilder(combinationName=" + this.getCombinationName() + ", binomials=" + this.getBinomials() + ", exclusions=" + this.getExclusions() + ")";
    }

    private class Binomial {
        private final String name;
        private final double pTrue;

        @java.beans.ConstructorProperties({"name", "pTrue"})
        public Binomial(String name, double pTrue) {
            this.name = name;
            this.pTrue = pTrue;
        }

        public double getPFalse() {
            return 1d - pTrue;
        }

        public String getName() {
            return this.name;
        }

        public double getPTrue() {
            return this.pTrue;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Binomial)) return false;
            final Binomial other = (Binomial) o;
            final Object this$name = this.getName();
            final Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            if (Double.compare(this.getPTrue(), other.getPTrue()) != 0) return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final long $pTrue = Double.doubleToLongBits(this.getPTrue());
            result = result * PRIME + (int) ($pTrue >>> 32 ^ $pTrue);
            return result;
        }

        public String toString() {
            return "BinomialCombGeneratorBuilder.Binomial(name=" + this.getName() + ", pTrue=" + this.getPTrue() + ")";
        }
    }

    private final String combinationName;
    private final List<Binomial> binomials = new ArrayList<>();
    private final Set<Case> exclusions = new HashSet<>();

    public void addBinomial(String name, double pTrue) {
        assert exclusions.isEmpty();
        assert pTrue >= 0d;
        assert pTrue <= 1d;
        binomials.add(new Binomial(name, pTrue));
    }

    public void excludeCombination(boolean... combinations) {
        assert combinations.length == binomials.size();
        exclusions.add(new Case(0, combinations));
    }

    private static class Case {
        private double proba;
        private boolean[] branch;

        @java.beans.ConstructorProperties({"proba", "branch"})
        public Case(double proba, boolean[] branch) {
            this.proba = proba;
            this.branch = branch;
        }

        public double getProba() {
            return this.proba;
        }

        public boolean[] getBranch() {
            return this.branch;
        }

        public void setProba(double proba) {
            this.proba = proba;
        }

        public void setBranch(boolean[] branch) {
            this.branch = branch;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Case)) return false;
            final Case other = (Case) o;
            if (!other.canEqual((Object) this)) return false;
            if (Double.compare(this.getProba(), other.getProba()) != 0) return false;
            if (!Arrays.equals(this.getBranch(), other.getBranch())) return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $proba = Double.doubleToLongBits(this.getProba());
            result = result * PRIME + (int) ($proba >>> 32 ^ $proba);
            result = result * PRIME + Arrays.hashCode(this.getBranch());
            return result;
        }

        protected boolean canEqual(Object other) {
            return other instanceof Case;
        }

        public String toString() {
            return "BinomialCombGeneratorBuilder.Case(proba=" + this.getProba() + ", branch=" + Arrays.toString(this.getBranch()) + ")";
        }
    }




    public Generator<boolean[]> build() {
        return generatorBuilder().build();
    }

    public EnumeratedDistributionGeneratorBuilder<boolean[]> generatorBuilder() {
        // we build a boolean Tree with each branch as a proba;
        int numCases = 1 << binomials.size();

        Collection<LinearConstraint> constraints = new ArrayList<>();

        addConstraintsThanBlendedProbaForEachBinomialMustBeRespected(numCases, constraints, 0);
        addConstraintThanSumOfProbaOfAllCasesMustBe1(numCases, constraints, 0);
        addConstraintsThatEachProbaIsNonNegative(numCases, constraints, 0);
        addContraintsThatExcludedCombinationProbaIs0(numCases, constraints, 0);

        // we maximize the cases where a lot of entry are true (so for the usage a lot of null generators, so less generation)


        PointValuePair optSolution = null;
        try {
            double[] coefs = IntStream.range(0, numCases).mapToDouble(i -> Integer.bitCount(i)).toArray();
            LinearObjectiveFunction f = new LinearObjectiveFunction(coefs, 0);
            SimplexSolver solver = new SimplexSolver();
            optSolution = solver.optimize(new MaxIter(10000), f, new
                    LinearConstraintSet(constraints), GoalType.MAXIMIZE, new NonNegativeConstraint(true));
        } catch (TooManyIterationsException e) {
            throw new RuntimeException("Should not happen", e);
        } catch (NoFeasibleSolutionException e) {
            binomials.stream().map(Binomial::getName).toArray(String[]::new);
            log.warn("The blended probabilities of ({}) constrained by {} cannot be respected. We minimize the error.", binomials.stream().map(Binomial::getName).toArray(String[]::new), combinationName);
            // lets try to minimize the error then
            constraints.clear();

            int numErrorVars = binomials.size() * 2;

            addConstraintThanSumOfProbaOfAllCasesMustBe1(numCases, constraints, numErrorVars);
            addConstraintsThatEachProbaIsNonNegative(numCases, constraints, numErrorVars);
            addContraintsThatExcludedCombinationProbaIs0(numCases, constraints, numErrorVars);

            // add errors are positive
            for (int i = 0; i < numErrorVars; i++) {
                double[] coefs = new double[numCases+numErrorVars];
                coefs[numCases+1] = 1d;
            //    constraints.add(new LinearConstraint(coefs, Relationship.GEQ, 0d));
            }

            for (int i = 0, binomialsSize1 = binomials.size(); i < binomialsSize1; i++) {
                Binomial b1 = binomials.get(i);

                int mask1 = 1 << i;

                double[] coefsTrue = new double[numCases+numErrorVars];
                double[] coefsFalse = new double[numCases+numErrorVars];
                for (int j = 0; j < numCases; j++) {
                    if ((j & mask1) == mask1) { // true case
                        coefsTrue[j] = 1;
                    } else {
                        coefsFalse[j] = 1;
                    }
                }
                coefsTrue[numCases+i*2] = -1; // error var
                coefsFalse[numCases+i*2+1] = -1; // error var

                constraints.add(new LinearConstraint(coefsTrue, Relationship.LEQ, b1.getPTrue()));
                constraints.add(new LinearConstraint(coefsFalse, Relationship.LEQ, b1.getPFalse()));
            }

            double[] coefs = new double[numCases+numErrorVars];
            for (int i = 0; i < numErrorVars; i++) {
                coefs[numCases+i] = 1d;
            }

            LinearObjectiveFunction f = new LinearObjectiveFunction(coefs, 0d);
            SimplexSolver solver = new SimplexSolver();
            optSolution = solver.optimize(new MaxIter(10000), f, new
                    LinearConstraintSet(constraints), GoalType.MINIMIZE, new NonNegativeConstraint(true));
        }

        double[] solution = optSolution.getPoint();

        for (int i = 0, binomialsSize1 = binomials.size(); i < binomialsSize1; i++) {
            Binomial b1 = binomials.get(i);

            int mask1 = 1 << i;

            double actualBlended = 0;
            for (int j = 0; j < numCases; j++) {
                if ((j & mask1) == mask1) { // true case
                    actualBlended += solution[j];
                }
            }

            double pTrue = b1.getPTrue();

            if (!Precision.equals(pTrue, actualBlended, 1.0e-6)) {
                log.warn("Actual probability of {} = {} will deviate from the one of stats {}", b1.getName(), actualBlended, pTrue );
            }
        }


        List<Double> weights = new ArrayList<>();
        List<boolean[]> values = new ArrayList<>();

        for (int i = 0; i < numCases; i++) {
            if (solution[i] == 0d) continue;

            boolean[] booleans = new boolean[binomials.size()];
            for (int j = 0; j < booleans.length; j++) {
                int mask = 1 << j;
                booleans[j] = (i & mask) == mask;
            }
            weights.add(solution[i]);
            values.add(booleans);
        }

        return enumerated(boolean[].class)
                .weights(weights.stream().mapToDouble(Double::doubleValue).toArray())
                .values(values.stream().toArray(boolean[][]::new));
    }

    private void addContraintsThatExcludedCombinationProbaIs0(int numCases, Collection<LinearConstraint> constraints, int numErrorVars) {
        for (Case ex: exclusions) {
            int idx = 0;
            boolean[] branch = ex.branch;
            for (int i = 0, branchLength = branch.length; i < branchLength; i++) {
                if (branch[i]) idx += 1 << i;
            }

            double[] coefsOne = new double[numCases+numErrorVars];
            coefsOne[idx] = 1d;
            constraints.add(new LinearConstraint(coefsOne, Relationship.EQ, 0d));
        }
    }

    private void addConstraintsThatEachProbaIsNonNegative(int numCases, Collection<LinearConstraint> constraints, int numErrorVars) {
        IntStream.range(0, numCases)
                .forEach(idx -> {
                    double[] coefsOne = new double[numCases+numErrorVars];
                    coefsOne[idx] = 1d;
                    constraints.add(new LinearConstraint(coefsOne, Relationship.GEQ, 0d));
                });
    }

    private void addConstraintsThanBlendedProbaForEachBinomialMustBeRespected(int numCases, Collection<LinearConstraint> constraints, int numErrorVars) {
        for (int i = 0, binomialsSize = binomials.size(); i < binomialsSize; i++) {
            Binomial b = binomials.get(i);

            int mask = 1 << i;

            double[] coefsTrue = new double[numCases+numErrorVars];
            double[] coefsFalse = new double[numCases+numErrorVars];
            for (int j = 0; j < numCases; j++) {
                if ((j & mask) == mask) { // true case
                    coefsTrue[j] = 1;
                } else {
                    coefsFalse[j] = 1;
                }
            }

            constraints.add(new LinearConstraint(coefsTrue, Relationship.EQ, b.getPTrue()));
            constraints.add(new LinearConstraint(coefsFalse, Relationship.EQ, b.getPFalse()));
        }
    }

    private void addConstraintThanSumOfProbaOfAllCasesMustBe1(int numCases, Collection<LinearConstraint> constraints, int numErrorVars) {
        double[] coefsAllOne = new double[numCases+numErrorVars];
        for (int i = 0; i < numCases; i++) {
            coefsAllOne[i] = 1d;
        }
        constraints.add(new LinearConstraint(coefsAllOne, Relationship.EQ, 1d));
    }


}
