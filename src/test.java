import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class test {
    public static void main(String[] args) throws ParseException {
        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(1);
        messages.add(2);
        messages.add(3);
        messages.add(1);
        messages.add(5);
        messages.add(6);
        messages.add(7);
        ArrayList<Long> times = new ArrayList<>();
        times.add((long)1110);
        times.add((long)2221);
        times.add((long)3332);
        times.add((long)4440);
        times.add((long)5550);
        times.add((long)6660);
        times.add((long)7770);
        List<Long> list = new ArrayList<>();
        list = times.subList(0,5);
        //ArrayList<Long> testList = new ArrayList<>();
        //ArrayList<Long> testList = new ArrayList<>(list);
        ArrayList<Long> testList = new ArrayList<>();
        testList = new ArrayList<>(list);
        //testlist = ArrayList(list)
        System.out.println(testList.getClass());
        System.out.println(list.getClass());
        double[] result = calculateGap(messages, times);
        double mean = result[0];
        double sd = result[1];
        System.out.println(mean);
        System.out.println(sd);
        double a = 0.988888;
        System.out.println(""+a*100);
        String channel = "fast";
        int i = 0;
        String subscribe_topic = "counter/fast/q0";
        int qos = Character.getNumericValue(subscribe_topic.split("/")[2].charAt(1));
        System.out.println("qos"+qos);
        System.out.println((double) 4055/5692);

    }

    public static double[] calculateGap(ArrayList<Integer> messages, ArrayList<Long> times) {
        // we don't need consider the last element in the list
        ArrayList<Long> gaps = new ArrayList<>();
        int count = 0;
        long totalGap = 0;
        for (int i = 0; i < messages.size() - 1; i++) {
            int current = messages.get(i);
            int next = messages.get(i+1);
            // if the next element is not duplicate and not ooo, it's ok to add the gap
            if (messages.indexOf(next) == i+1 & current < next) {
                long current_time = times.get(i);
                long next_time = times.get(i+1);
                gaps.add(next_time - current_time);
                totalGap += (next_time - current_time);
                count += 1;
            } else {
                // if duplication/gap, skip the next
                i += 1;
            }
        }
        // return totalGap/count which is a long
        double meanGap = (double)totalGap/count;

        //calculate the variance
        double difference = 0;
        for (int i = 0; i < gaps.size(); i++) {
            difference += (gaps.get(i) - meanGap)* (gaps.get(i) - meanGap);
        }
        double sd = Math.sqrt(difference/gaps.size());
        double[] result = new double[2];
        System.out.println("totalGap "+totalGap);
        System.out.println("count "+count);
        //System.out.println("0 "+totalGap/count);
        result[0] = (double)totalGap/count;
        result[1] = sd;
        return result;
    }

    public static ArrayList<Double> removeTimeDuplicate(ArrayList<Double> times, ArrayList<Integer> messages, ArrayList<Integer> unique) {
        ArrayList<Double> nondupTime = new ArrayList<>();
        for (int i = 0; i < unique.size(); i++) {
            int index = messages.indexOf(unique.get(i));
            nondupTime.add(times.get(index));
        }
        return nondupTime;
    }

    /* calculate the loss */
    // 1,2,3,4,4,7,8,9
    // 9-1
    public static int oooMessage(ArrayList<Integer> messages) {
        int original_size = messages.size();
        for (int i = 0; i < messages.size(); i++) {
            if (i != messages.size() - 1) {
                // it is not possible get the equal in here
                if (messages.get(i) > messages.get(i+1)) {
                    messages.remove(i);
                }
            }
        }
        return original_size - messages.size();
    }
}
