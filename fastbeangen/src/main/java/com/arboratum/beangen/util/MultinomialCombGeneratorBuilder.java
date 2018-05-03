package com.arboratum.beangen.util;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.core.EnumeratedDistributionGeneratorBuilder;
import org.apache.commons.math3.exception.TooManyIterationsException;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.util.MathArrays;
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
public class MultinomialCombGeneratorBuilder {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MultinomialCombGeneratorBuilder.class);

    @java.beans.ConstructorProperties({"combinationName"})
    public MultinomialCombGeneratorBuilder(String combinationName) {
        this.combinationName = combinationName;
    }

    public String getCombinationName() {
        return this.combinationName;
    }

    public List<Multinomial> getMultinomials() {
        return this.multinomials;
    }

    public Set<Case> getExclusions() {
        return this.exclusions;
    }


    public String toString() {
        return "MultinomialCombGeneratorBuilder(combinationName=" + this.getCombinationName() + ", multinomials=" + this.getMultinomials() + ", exclusions=" + this.getExclusions() + ")";
    }

    private class Multinomial {
        private final String name;
        private final String[] values;
        private final double[] p;

        @java.beans.ConstructorProperties({"name", "p"})
        public Multinomial(String name, String[] values, double[] p) {
            this.name = name;
            this.values = values;
            this.p = p;
        }

        public String getName() {
            return name;
        }
    }

    private final String combinationName;
    private final List<Multinomial> multinomials = new ArrayList<>();
    private final Set<Case> exclusions = new HashSet<>();
    private final List<LTConstraint> additionalConstraints = new ArrayList<>();


    public void addMultinomial(String name, String[] values, double[] p) {
        assert exclusions.isEmpty();
        assert additionalConstraints.isEmpty();

        p = MathArrays.normalizeArray(p, 1.0);
        multinomials.add(new Multinomial(name, values, p));
    }

    public void excludeCombination(int... combinations) {
        assert combinations.length == multinomials.size();

        exclusions.add(new Case(0, combinations));
    }

    public void addLTConstraint(int[] a, int[] b) {
        assert a.length == multinomials.size();
        assert b.length == multinomials.size();

        additionalConstraints.add(new LTConstraint(a, b));
    }

    private static class LTConstraint {
        private int[] a;
        private int[] b;

        public LTConstraint(int[] a, int[] b) {
            this.a = a;
            this.b = b;
        }
    }

    private static class Case {
        private double proba;
        private int[] branch;

        @java.beans.ConstructorProperties({"proba", "branch"})
        public Case(double proba, int[] branch) {
            this.proba = proba;
            this.branch = branch;
        }

        public double getProba() {
            return this.proba;
        }

        public int[] getBranch() {
            return this.branch;
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




    public Generator<int[]> build() {
        return generatorBuilder().build();
    }

    public EnumeratedDistributionGeneratorBuilder<int[]> generatorBuilder() {
        OptimizedCombinationDistro optimizedCombinationDistro = buildOptimizedCombDistro();
        List<Double> weights = optimizedCombinationDistro.getWeights();
        List<int[]> values = optimizedCombinationDistro.getValues();

        return enumerated(int[].class)
                .weights(weights.stream().mapToDouble(Double::doubleValue).toArray())
                .values(values.stream().toArray(int[][]::new));
    }

    public OptimizedCombinationDistro buildOptimizedCombDistro() {
        return new OptimizedCombinationDistro().invoke();
    }

    private int convertToCoef(int[] branch) {
        int result = 0;
        int rank = 1;
        for (int i = 0, binomialsSize = multinomials.size(); i < binomialsSize; i++) {
            Multinomial multinomial = multinomials.get(i);
            result += branch[i] * rank;
            rank *= multinomial.p.length;
        }
        return result;
    }

    private List<int[]> generateAllBranches() {
        List<int[]> current = Collections.singletonList(new int[0]);
        for (int i = 0, binomialsSize = multinomials.size(); i < binomialsSize; i++) {
            Multinomial multinomial = multinomials.get(i);
            List<int[]> next = new ArrayList<>();

            for (int[] prev : current) {
                for (int j = 0; j < multinomial.values.length; j++) {
                    int[] e = Arrays.copyOf(prev, i + 1);
                    e[i] = j;
                    next.add(e);
                }
            }
            current = next;
        }
        return current;
    }


    public class OptimizedCombinationDistro {
        private List<Double> weights;
        private List<int[]> values;

        public List<Double> getWeights() {
            return weights;
        }

        public List<int[]> getValues() {
            return values;
        }

        public OptimizedCombinationDistro invoke() {
            int numCases = getMultinomials().stream()
                    .mapToInt(m -> m.values.length)
                    .reduce(1, (left, right) -> left * right);

            Collection<LinearConstraint> constraints = new ArrayList<>();
            List<int[]> allbranches = generateAllBranches();

            addConstraintsThanBlendedProbaForEachMultinomialMustBeRespected(numCases, constraints, 0);
            addConstraintThanSumOfProbaOfAllCasesMustBe1(numCases, constraints, 0);
            addConstraintsThatEachProbaIsNonNegative(numCases, constraints, 0);
            addContraintsThatExcludedCombinationProbaIs0(numCases, constraints, 0);
            addAdditionalContraints(numCases, constraints, 0);

            PointValuePair optSolution = null;
            try {
                // prioritize the combinations that have a priori the highest probalility
                double[] coefs = new double[numCases];

                Arrays.fill(coefs, 1d);

                LinearObjectiveFunction f = new LinearObjectiveFunction(coefs, 0);
                SimplexSolver solver = new SimplexSolver();
                optSolution = solver.optimize(new MaxIter(10000), f, new
                        LinearConstraintSet(constraints), GoalType.MAXIMIZE, new NonNegativeConstraint(true));
            } catch (TooManyIterationsException e) {
                throw new RuntimeException("Should not happen", e);
            } catch (NoFeasibleSolutionException e) {
                log.warn("The blended probabilities of ({}) constrained by {} cannot be respected. We minimize the error.", multinomials.stream().map(Multinomial::getName).toArray(String[]::new), combinationName);
                // lets try to minimize the error then
                constraints.clear();

                int numErrorVars = numCases;

                addConstraintThanSumOfProbaOfAllCasesMustBe1(numCases, constraints, numErrorVars);
                addConstraintsThatEachProbaIsNonNegative(numCases, constraints, numErrorVars);
                addContraintsThatExcludedCombinationProbaIs0(numCases, constraints, numErrorVars);
                addAdditionalContraints(numCases, constraints, numErrorVars);

                int errorIndex = 0;
                for (int i = 0, binomialsSize = multinomials.size(); i < binomialsSize; i++) {
                    Multinomial b = multinomials.get(i);

                    for (int j = 0; j < b.p.length; j++) {

                        // list all branches with value j
                        int finalI = i;
                        int finalJ = j;
                        double[] coefsTrue = new double[numCases+numErrorVars];

                        allbranches.stream()
                                .filter(branch -> branch[finalI] == finalJ)
                                .mapToInt(branch -> convertToCoef(branch))
                                .forEach(coef -> coefsTrue[coef] = 1d);

                        coefsTrue[numCases + errorIndex] = -1d;

                        errorIndex++;

                        constraints.add(new LinearConstraint(coefsTrue, Relationship.LEQ, b.p[j]));
                    }
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

            for (int i = 0, binomialsSize1 = multinomials.size(); i < binomialsSize1; i++) {
                Multinomial b1 = multinomials.get(i);

                for (int j = 0; j < b1.p.length; j++) {
                    String value = b1.values[j];
                    double expectedP = b1.p[j];

                    // list all branches with value j
                    int finalI = i;
                    int finalJ = j;

                    double actualBlended = allbranches.stream()
                            .filter(branch -> branch[finalI] == finalJ)
                            .mapToInt(branch -> convertToCoef(branch))
                            .mapToDouble(coef -> solution[coef])
                            .sum();

                    if (!Precision.equals(expectedP, actualBlended, 1.0e-6)) {
                        log.warn("Actual probability of {}/{} = {} will deviate from the one of stats {}", b1.getName(), value, actualBlended, expectedP );
                    }
                }
            }


            weights = new ArrayList<>();
            values = new ArrayList<>();

            for (int i = 0; i < allbranches.size(); i++) {
                int[] branch = allbranches.get(i);
                int solIndex = convertToCoef(branch);
                if (solution[solIndex] == 0d) continue;

                weights.add(solution[solIndex]);
                values.add(branch);
            }
            return this;
        }

        private void addAdditionalContraints(int numCases, Collection<LinearConstraint> constraints, int numErrorVars) {
            additionalConstraints.stream().forEach(c -> {
                double[] coefs = new double[numCases+numErrorVars];
                coefs[convertToCoef(c.a)] = 1d;
                coefs[convertToCoef(c.b)] = -1d;
                constraints.add(new LinearConstraint(coefs, Relationship.GEQ, 0d));
            });
        }

        private void addConstraintsThanBlendedProbaForEachMultinomialMustBeRespected(int numCases, Collection<LinearConstraint> constraints, int numErrorVars) {
            List<int[]> allbranches = generateAllBranches();
            for (int i = 0, binomialsSize = multinomials.size(); i < binomialsSize; i++) {
                Multinomial b = multinomials.get(i);

                for (int j = 0; j < b.p.length; j++) {

                    // list all branches with value j
                    int finalI = i;
                    int finalJ = j;
                    double[] coefsTrue = new double[numCases+numErrorVars];

                    allbranches.stream()
                            .filter(branch -> branch[finalI] == finalJ)
                            .mapToInt(branch -> convertToCoef(branch))
                            .forEach(coef -> coefsTrue[coef] = 1d);

                    constraints.add(new LinearConstraint(coefsTrue, Relationship.EQ, b.p[j]));
                }
            }
        }

        private void addConstraintThanSumOfProbaOfAllCasesMustBe1(int numCases, Collection<LinearConstraint> constraints, int numErrorVars) {
            double[] coefsAllOne = new double[numCases+numErrorVars];
            for (int i = 0; i < numCases; i++) {
                coefsAllOne[i] = 1d;
            }
            constraints.add(new LinearConstraint(coefsAllOne, Relationship.EQ, 1d));
        }

        private void addConstraintsThatEachProbaIsNonNegative(int numCases, Collection<LinearConstraint> constraints, int numErrorVars) {
            IntStream.range(0, numCases)
                    .forEach(idx -> {
                        double[] coefsOne = new double[numCases+numErrorVars];
                        coefsOne[idx] = 1d;
                        constraints.add(new LinearConstraint(coefsOne, Relationship.GEQ, 0d));
                    });
        }

        private void addContraintsThatExcludedCombinationProbaIs0(int numCases, Collection<LinearConstraint> constraints, int numErrorVars) {
            for (Case ex: exclusions) {
                double[] coefsOne = new double[numCases+numErrorVars];
                coefsOne[convertToCoef(ex.branch)] = 1d;
                constraints.add(new LinearConstraint(coefsOne, Relationship.EQ, 0d));
            }
        }
    }
}
