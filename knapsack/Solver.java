import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class Element{
    private int value;
    private int weight;

    public Element(int value, int weight) {
        this.value = value;
        this.weight = weight;
    }

    public int value(){
        return value;
    }

    public int weight(){
        return weight;
    }
}

class InputData{
    private final Element[] elements;
    private final int begin;
    private final int end;

    public InputData(Element[] elements) {
        this.elements = elements;
        this.begin = 0;
        this.end = elements.length;
    }

    public InputData(Element[] elements, int begin, int end) {
        this.elements = elements;
        this.begin = begin;
        this.end = end;
        if(begin < 0 || end > elements.length || begin > end){
            throw new ArrayIndexOutOfBoundsException("sub-range index out of range");
        }
    }

    public Element getElement(int idx){
        return elements[idx + begin];
    }

    public InputData getSubRange(int start, int last){
        return new InputData(elements, this.begin + start, this.begin + last);
    }

    public int size(){
        return end - begin;
    }
}

class ReorderComparator implements Comparator<Integer>{
    final public InputData data;

    public ReorderComparator(InputData data){
        this.data = data;
    }

    @Override
    public int compare(Integer o1, Integer o2) {
        return -Comparator.<Integer>naturalOrder().compare(o1, o2);
    }
}

class InputReorderUtil{
    ReorderComparator cmp;
    InputData input;
    Integer[] index;

    InputReorderUtil(ReorderComparator cmp) {
        this.cmp = cmp;
        this.input = cmp.data;
        this.index = new Integer[this.input.size()];
        for(int i = 0; i < cmp.data.size(); ++i){
            this.index[i] = i;
        }
        Arrays.sort(index, cmp);
    }

    InputData reorder(){
        Element[] elements = new Element[input.size()];
        for(int i = 0; i < input.size(); ++i){
            elements[i] = input.getElement(index[i]);
        }
        return new InputData(elements);
    }

    boolean[] backToOriginPick(boolean[] pick){
        if(pick.length != input.size()){
            throw new RuntimeException("pick size is not consistent with input size");
        }

        boolean[] pick_ans = new boolean[pick.length];
        for(int i = 0; i < input.size(); ++i){
            pick_ans[index[i]] = pick[i];
        }
        return pick_ans;
    }
}

class ProblemStat {
    private int size;
    private int capacity;
    private InputData input;

    public ProblemStat(int capacity, InputData input) {
        this.size = input.size();
        this.capacity = capacity;
        this.input = input;
    }

    public ProblemStat toSubProblem(int size, int capacity){
        if(!(size < this.size || capacity < this.capacity)){
            throw new RuntimeException("Logical Error: the size/capacity of sub-problem should be smaller.");
        }

        return new ProblemStat(capacity, this.input.getSubRange(0, size));
    }

    public ProblemStat toSubRangeProblem(int begin, int end, int capacity){
        int size = end - begin;
        if(!(size < this.size || capacity < this.capacity)){
            throw new RuntimeException("Logical Error: the size/capacity of sub-problem should be smaller.");
        }

        return new ProblemStat(capacity, this.input.getSubRange(begin, end));
    }

    public Element getElement(int idx){
        return input.getElement(idx);
    }

    public Element getLastElement(){
        return input.getElement(getSize() - 1);
    }

    public int getSize(){
        return size;
    }

    public int getCapacity(){
        return capacity;
    }

    public InputData getInputData(){
        return input;
    }
}

class ProblemAnswer{
    ProblemStat stat; // only serves as a reference to the problem statics, can't modify

    public int opt_value;
    public boolean [] pick;
    boolean isAccurate = false;

    // copy a answer
    public ProblemAnswer(ProblemStat stat, int opt_value, boolean[] pick) {
        this.stat = stat;
        this.opt_value = opt_value;
        this.pick = pick;
    }

    static public ProblemAnswer CreateDummyAnswer(ProblemStat stat){
        boolean[] pick = new boolean[stat.getSize()];
        Arrays.fill(pick, false);
        return new ProblemAnswer(stat, 0, pick);
    }

    public void update(int idx, boolean pick_this, int d_opt_value){
        opt_value += d_opt_value;

        if(!((pick[idx] && !pick_this && d_opt_value < 0) || // unpick, loss value
                (!pick[idx] && pick_this && d_opt_value > 0) || // pick,  gain value
                (d_opt_value == 0)  // without change
        )){
            throw new RuntimeException("Logical Error: the update logic is wrong.");
        }

        pick[idx] = pick_this;
    }

    public void dump(){
        System.out.println(opt_value + " " + (isAccurate ? "1" : "0"));
        for (boolean pick_this : pick)
            System.out.print((pick_this ? "1" : "0") + " ");
        System.out.println("");
    }

    void verify(){
        int acc_value = 0;
        int acc_weight = 0;
        for(int i = 0; i < pick.length; ++i){
            if(pick[i]){
                Element element = stat.getElement(i);
                acc_value += element.value();
                acc_weight += element.weight();
            }
        }
        if(acc_weight > stat.getCapacity() || acc_value != opt_value){
            throw new RuntimeException("Verify Failed");
        }
    }

    void setIsAccurate(){
        isAccurate = true;
    }
}

class KnapsackSolver{
    final ProblemStat stat;

    public KnapsackSolver(ProblemStat stat) {
        this.stat = stat;
    }

    public ProblemAnswer solveByAlwaysTryPickLast(ProblemStat local_stat){
        ProblemAnswer ans = ProblemAnswer.CreateDummyAnswer(local_stat);

        while (local_stat.getSize() > 0 && local_stat.getCapacity() > 0){
            int idx = local_stat.getSize() - 1;

            Element last = local_stat.getLastElement();
            int v = last.value();
            int w = last.weight();

            int next_size = local_stat.getSize() - 1;
            int next_capacity = local_stat.getCapacity();

            if(local_stat.getCapacity() >= w){
                // can pick this one
                next_capacity = local_stat.getCapacity() - w;
                ans.update(idx, true, v);
            }

            local_stat = local_stat.toSubProblem(next_size, next_capacity);
        }
        ans.verify();
        return ans;
    }

    public ProblemAnswer solve(){
        return solveByAlwaysTryPickLast(new ProblemStat(stat.getCapacity(), stat.getInputData()));
    }
}

class CmpByValuePerWeight extends ReorderComparator{
    public CmpByValuePerWeight(InputData data){
        super(data);
    }

    @Override
    public int compare(Integer idx1, Integer idx2) {
        Element elem1 = this.data.getElement(idx1);
        Element elem2 = this.data.getElement(idx2);
        if(elem1.weight() == 0 && elem2.weight() == 0){
            return Comparator.<Integer>naturalOrder().compare(elem1.value(), elem2.value());
        }
        if(elem1.weight() == 0)
            return 1;
        if(elem2.weight() == 0)
            return -1;
        return Comparator.<Double>naturalOrder().compare(
                (double) elem1.value() / (double) elem1.weight(),
                (double) elem2.value() / (double) elem2.weight());
    }
}

class GreedyImpl extends KnapsackSolver{
    InputReorderUtil reorderUtil;

    public GreedyImpl(ProblemStat stat) {
        super(stat);
        reorderUtil = new InputReorderUtil(new CmpByValuePerWeight(stat.getInputData()));
    }

    public ProblemAnswer solve(){
        ProblemAnswer ans = solveByAlwaysTryPickLast(new ProblemStat(stat.getCapacity(), reorderUtil.reorder()));

        ans.pick = reorderUtil.backToOriginPick(ans.pick);
        ans.stat = stat;
        ans.verify();

        return ans;
    }
}

class DynamicProgImpl extends KnapsackSolver{
    static class MemorizeEntry{
        public boolean computed;
        public int opt_value;
        public int last_capacity;

        MemorizeEntry(){
            computed = true;
            opt_value = 0;
            last_capacity = 0;
        }
    }

    MemorizeEntry[][] table;

    public DynamicProgImpl(ProblemStat stat) {
        super(stat);

        int size = stat.getSize();
        int capacity = stat.getCapacity();

        table = new MemorizeEntry[size + 1][capacity + 1];
        for(int i = 0; i <= size; ++i){
            table[i] = new MemorizeEntry[capacity + 1];
            for(int j = 0; j <= capacity; ++j){
                table[i][j] = new MemorizeEntry();
            }
        }

        for(int i = 0; i <= size; ++i) {
            table[i][0].last_capacity = 0;
        }

        for(int j = 0; j <= capacity; ++j){
            table[0][j].last_capacity = j;
        }
    }

    void initTable(){
        for(int i = 1; i <= stat.getSize(); ++ i){
            for(int j = 1; j <= stat.getCapacity(); ++j){
                table[i][j].computed = false;
            }
        }
    }

    void solveSubProblem(int size, int capacity){
        if(size == 0 || capacity == 0 || table[size][capacity].computed){
            return;
        }

        Element last = stat.getElement(size - 1);
        int v = last.value();
        int w = last.weight();

        solveSubProblem(size - 1, capacity);
        if(capacity >= w) {
            solveSubProblem(size - 1, capacity - w);

            boolean selector = table[size - 1][capacity].opt_value > table[size - 1][capacity - w].opt_value + v;

            table[size][capacity].opt_value = selector ?
                    table[size - 1][capacity].opt_value :
                    table[size - 1][capacity - w].opt_value + v
            ;

            table[size][capacity].last_capacity = selector ?
                    capacity :
                    capacity - w
            ;
        } else {
            table[size][capacity].opt_value = table[size - 1][capacity].opt_value;
            table[size][capacity].last_capacity = capacity;
        }
        table[size][capacity].computed = true;
    }

    ProblemAnswer backtrack(){
        int size = stat.getSize();
        int capacity = stat.getCapacity();

        ProblemAnswer ans = ProblemAnswer.CreateDummyAnswer(stat);
        ans.opt_value = table[size][capacity].opt_value;


        int cur_capacity = capacity;
        for(int cur_size = size; cur_size > 0; --cur_size){
            //int idx = stat.getReorderedIdx(cur_size - 1);
            int idx = cur_size - 1;
            ans.pick[idx] = table[cur_size][cur_capacity].last_capacity != cur_capacity;
            cur_capacity = table[cur_size][cur_capacity].last_capacity;
        }

        ans.setIsAccurate();
        return ans;
    }

    public ProblemAnswer solve(){
        initTable();
        solveSubProblem(stat.getSize(), stat.getCapacity());

        ProblemAnswer ans = backtrack();

        ans.verify();
        return ans;
    }
}

class DynamicProgLowMemoryImpl extends KnapsackSolver{
    InputReorderUtil reorderUtil;
    ProblemStat local_stat;

    int[] table;
    int last_capacity;

    public DynamicProgLowMemoryImpl(ProblemStat stat) {
        super(stat);

        int size = stat.getSize();
        int capacity = stat.getCapacity();

        table = new int[capacity + 1];

        reorderUtil = new InputReorderUtil(new CmpByValuePerWeight(stat.getInputData()));
    }

    void solveSubProblem(int size, int capacity){
        for(int i = 0; i <= capacity; ++i){
            table[i] = 0;
        }

        for(int i = 0; i < size - 1; ++i){
            Element element = local_stat.getElement(i);
            int v = element.value();
            int w = element.weight();


            for(int j = capacity; j >= w; --j){
                if(table[j - w] + v > table[j]){
                    table[j] = table[j - w] + v;
                }
            }
        }

        Element element = local_stat.getElement(size - 1);
        int v = element.value();
        int w = element.weight();
        last_capacity = capacity;
        if(capacity >= w) {
            if (table[capacity - w] + v > table[capacity]) {
                table[capacity] = table[capacity - w] + v;
                last_capacity = capacity - w;
            }
        }
    }

    int getOnlyOptValue(){
        int size = local_stat.getSize();
        int capacity = local_stat.getCapacity();

        solveSubProblem(size, capacity);

        return table[capacity];
    }

    boolean[] backtrack(){
        int size = local_stat.getSize();
        int capacity = local_stat.getCapacity();
        boolean[] pick = new boolean[size];

        backtrackIter(pick, size, capacity);

        return pick;
    }

    void backtrackIter(boolean[] pick, int size, int capacity){
        if(size == 0 || capacity == 0)
            return;

        solveSubProblem(size, capacity);

        int idx = size - 1;
        if(last_capacity == capacity){
            pick[idx] = false;
            backtrackIter(pick, size - 1, capacity);
        } else {
            pick[idx] = true;
            backtrackIter(pick, size - 1, last_capacity);
        }
    }

    public ProblemAnswer solve(){
        this.local_stat = new ProblemStat(stat.getCapacity(), reorderUtil.reorder());
        ProblemAnswer ans = ProblemAnswer.CreateDummyAnswer(stat);

        boolean[] pick = backtrack();

        ans.pick = reorderUtil.backToOriginPick(pick);
        ans.stat = stat;
        ans.opt_value = getOnlyOptValue();
        ans.verify();

        return ans;
    }
}

class BranchAndBoundImpl extends KnapsackSolver{
    static class RelexedProblemEstimation {
        int[] suffixValueSum;
        int[] suffixWeightSum;
        double[] valuePerWeight;

        RelexedProblemEstimation(InputData inputData){
            int size = inputData.size();

            suffixValueSum = new int[size + 1];
            suffixWeightSum = new int[size + 1];

            suffixValueSum[size] = 0;
            suffixWeightSum[size] = 0;
            for(int i = size - 1; i >= 0; --i){
                Element element = inputData.getElement(i);

                suffixValueSum[i] = suffixValueSum[i + 1] + element.value();
                suffixWeightSum[i] = suffixWeightSum[i + 1] + element.weight();
            }

            valuePerWeight = new double[size];
            for(int i = 0; i < size; ++i){
                Element element = inputData.getElement(i);
                valuePerWeight[i] = (double) element.value() / (double) element.weight();
            }
        }

        // size: leftmost size element's relaxed estimation
        // capacity: current acceptable max capacity
        int estimate(int size, int capacity){
            int bias_v = suffixValueSum[size];
            int bias_w = suffixWeightSum[size];

            int idx = size - 1;
            while (idx >= 0){
                int cur_capacity = suffixWeightSum[idx] - bias_w;
                if(cur_capacity > capacity){
                    break;
                }
                --idx;
            }
            int used_capacity = suffixWeightSum[idx + 1] - bias_w;
            int opt_value = suffixValueSum[idx + 1] - bias_v;
            if(idx == -1){
                return opt_value;
            } else {
                return opt_value + (int)(valuePerWeight[idx] * (capacity - used_capacity));
            }
        }
    }
    static final int maxTolerateSec = 5;

    InputReorderUtil reorderUtil;
    long startTime;

    ProblemAnswer currBest;

    ProblemAnswer acc;
    ProblemStat local_stat;
    RelexedProblemEstimation estimation;

    public BranchAndBoundImpl(ProblemStat stat) {
        super(stat);
        reorderUtil = new InputReorderUtil(new CmpByValuePerWeight(stat.getInputData()));
    }

    boolean runOutOfTime(){
        return (System.nanoTime() - startTime) / 1e9 >= maxTolerateSec;
    }

    void iter(int size, int capacity){
        if(size == 0 || capacity == 0){
            if(acc.opt_value > currBest.opt_value) {
                // save result
                currBest.opt_value = acc.opt_value;
                for (int i = 0; i < local_stat.getSize(); ++i) {
                    currBest.pick[i] = acc.pick[i];
                }
            }
            return;
        }

        if(runOutOfTime() || (estimation.estimate(size, capacity) + acc.opt_value < currBest.opt_value)){
            // Bound!
            return;
        }

        int idx = size - 1;
        Element element = local_stat.getElement(idx);
        int v = element.value();
        int w = element.weight();
        if(capacity >= w){
            acc.opt_value += v;
            acc.pick[idx] = true;

            iter(size - 1, capacity - w);

            acc.pick[idx] = false;
            acc.opt_value -= v;
        }

        iter(size - 1, capacity);
    }

    public ProblemAnswer solve(){
        startTime = System.nanoTime();

        local_stat = new ProblemStat(stat.getCapacity(), reorderUtil.reorder());
        estimation = new RelexedProblemEstimation(local_stat.getInputData());

        currBest = solveByAlwaysTryPickLast(local_stat);
        acc = ProblemAnswer.CreateDummyAnswer(local_stat);

        iter(local_stat.getSize(), local_stat.getCapacity());

        currBest.verify();

        currBest.pick = reorderUtil.backToOriginPick(currBest.pick);
        currBest.stat = stat;
        currBest.verify();
        return currBest;
    }
}

class BranchAndBoundWithHintImpl extends KnapsackSolver{
    static class RelexedProblemEstimation {
        int[] suffixValueSum;
        int[] suffixWeightSum;
        double[] valuePerWeight;

        RelexedProblemEstimation(InputData inputData){
            int size = inputData.size();

            suffixValueSum = new int[size + 1];
            suffixWeightSum = new int[size + 1];

            suffixValueSum[size] = 0;
            suffixWeightSum[size] = 0;
            for(int i = size - 1; i >= 0; --i){
                Element element = inputData.getElement(i);

                suffixValueSum[i] = suffixValueSum[i + 1] + element.value();
                suffixWeightSum[i] = suffixWeightSum[i + 1] + element.weight();
            }

            valuePerWeight = new double[size];
            for(int i = 0; i < size; ++i){
                Element element = inputData.getElement(i);
                valuePerWeight[i] = (double) element.value() / (double) element.weight();
            }
        }

        // size: leftmost size element's relaxed estimation
        // capacity: current acceptable max capacity
        int estimate(int size, int capacity){
            int bias_v = suffixValueSum[size];
            int bias_w = suffixWeightSum[size];

            int idx = size - 1;
            while (idx >= 0){
                int cur_capacity = suffixWeightSum[idx] - bias_w;
                if(cur_capacity > capacity){
                    break;
                }
                --idx;
            }
            int used_capacity = suffixWeightSum[idx + 1] - bias_w;
            int opt_value = suffixValueSum[idx + 1] - bias_v;
            if(idx == -1){
                return opt_value;
            } else {
                return opt_value + (int)(valuePerWeight[idx] * (capacity - used_capacity));
            }
        }
    }
    static final int maxTolerateSec = 8;
    int knownBestOpt;

    InputReorderUtil reorderUtil;
    long startTime;

    ProblemAnswer currBest;

    ProblemAnswer acc;
    ProblemStat local_stat;
    RelexedProblemEstimation estimation;

    public BranchAndBoundWithHintImpl(ProblemStat stat) {
        super(stat);
        reorderUtil = new InputReorderUtil(new CmpByValuePerWeight(stat.getInputData()));
    }

    boolean runOutOfTime(){
        return (System.nanoTime() - startTime) / 1e9 >= maxTolerateSec;
    }

    void iter(int size, int capacity){
        if(size == 0 || capacity == 0){
            if(acc.opt_value > currBest.opt_value) {
                // save result
                currBest.opt_value = acc.opt_value;
                for (int i = 0; i < local_stat.getSize(); ++i) {
                    currBest.pick[i] = acc.pick[i];
                }
            }
            return;
        }

        if(runOutOfTime() || (estimation.estimate(size, capacity) + acc.opt_value < knownBestOpt)){
            // Bound!
            return;
        }

        int idx = size - 1;
        Element element = local_stat.getElement(idx);
        int v = element.value();
        int w = element.weight();
        if(capacity >= w){
            acc.opt_value += v;
            acc.pick[idx] = true;

            iter(size - 1, capacity - w);

            acc.pick[idx] = false;
            acc.opt_value -= v;
        }

        iter(size - 1, capacity);
    }

    public ProblemAnswer solve(){
        startTime = System.nanoTime();
        knownBestOpt = new DynamicProgLowMemoryImpl(stat).getOnlyOptValue();

        local_stat = new ProblemStat(stat.getCapacity(), reorderUtil.reorder());
        estimation = new RelexedProblemEstimation(local_stat.getInputData());

        currBest = solveByAlwaysTryPickLast(local_stat);
        acc = ProblemAnswer.CreateDummyAnswer(local_stat);

        iter(local_stat.getSize(), local_stat.getCapacity());

        currBest.verify();

        currBest.pick = reorderUtil.backToOriginPick(currBest.pick);
        currBest.stat = stat;
        currBest.verify();

        if(knownBestOpt == currBest.opt_value){
            currBest.setIsAccurate();
        }
        return currBest;
    }
}

/**
 * The class <code>Solver</code> is an implementation of a greedy algorithm to solve the knapsack problem.
 *
 */
public class Solver {
    static boolean debug = false;
    /**
     * The main class
     */
    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read the instance, solve it, and print the solution in the standard output
     */
    public static void solve(String[] args) throws IOException {
        String fileName = null;

        // get the temp file name
        for(String arg : args){
            if(arg.startsWith("-file=")){
                fileName = arg.substring(6);
            }
        }
        if(fileName == null)
            return;

        // read the lines out of the file
        List<String> lines = new ArrayList<String>();

        BufferedReader input =  new BufferedReader(new FileReader(fileName));
        try {
            String line = null;
            while (( line = input.readLine()) != null){
                lines.add(line);
            }
        }
        finally {
            input.close();
        }


        // parse the data in the file
        String[] firstLine = lines.get(0).split("\\s+");
        int items = Integer.parseInt(firstLine[0]);
        int capacity = Integer.parseInt(firstLine[1]);

        if(Solver.debug){
            System.out.printf("Have %d items, capacity = %d\n", items, capacity);
        }
        Element[] elements = new Element[items];

        for(int i=1; i < items+1; i++){
            String line = lines.get(i);
            String[] parts = line.split("\\s+");

            elements[i-1] = new Element(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }


        InputData inputData = new InputData(elements);
        ProblemStat stat = new ProblemStat(capacity, inputData);

        KnapsackSolver solver;
        ProblemAnswer ans;
        try {
            solver = new DynamicProgLowMemoryImpl(stat);
            ans = solver.solve();
        }
        catch (OutOfMemoryError e1) {
            try {
                solver = new BranchAndBoundImpl(stat);
                ans = solver.solve();
            } catch (StackOverflowError e2) {
                solver = new GreedyImpl(stat);
                ans = solver.solve();
            }
        }
        ans.dump();
    }
}
