import org.eclipse.paho.client.mqttv3.*;
import java.util.*;
import java.util.stream.Collectors;

public class PahoDemo implements MqttCallback {
    // The default setting is subscribed the topic 'counter/slowq1', if you want to change the running time, change the time. If you want to change the
    // topic, do it in main method. IF you want to publish the result, remove the 'publish code' comment in main method.
    // the time is 5 mins
    static int time = 5*60;
    static MqttClient client;
    /* An arraylist used to store received messages over 5 minutes. */
    static ArrayList<Integer> receivedMessage = new ArrayList<>();
    static ArrayList<Integer> receivedUniqueMessage = new ArrayList<>();
    static ArrayList<Long> receivedTime = new ArrayList<>();
    static int loss;
    static int ooo;
    static int duplicate;
    static double messageRate;
    static double lossRate;
    static double duplicateRate;
    static double oooRate;
    static double meanGap;
    static double sd;

    public PahoDemo() {
    }

    public class dataType {
        ArrayList<Integer> list1;
        ArrayList<Integer> lise2;
        ArrayList<Long> list11;
        ArrayList<Long> list22;

        public dataType (ArrayList<Integer> list1, ArrayList<Integer> lise2, ArrayList<Long> list11, ArrayList<Long> list22) {
            this.list1 = list1;
            this.lise2 = lise2;
            this.list11 = list11;
            this.list22 = list22;
        }
    }

    public static void main(String[] args) throws InterruptedException, MqttException {
        // CHANGE the detail when you run for different models
        String topic = "counter/slow/q1";
        new PahoDemo().doDemo(topic);
        new PahoDemo().calculate();
        // IF you need publish the result, remove the comment
        //new PahoDemo().publishResult(topic);
    }

    public void calculate() {
        dataType result = new PahoDemo().seperateList(receivedMessage, receivedTime);
        ArrayList<Integer> list1 = result.list1;
        ArrayList<Integer> list2 = result.lise2;
        ArrayList<Long> list11 = result.list11;
        ArrayList<Long> list22 = result.list22;
        if (list2.size() != 0 & list22.size() != 0) {
            ArrayList<Integer> receivedUniqueMessage1 = removeDuplicate(list1);
            ArrayList<Integer> receivedUniqueMessage2 = removeDuplicate(list2);
            int duplicate1 = list1.size() - receivedUniqueMessage1.size();
            int duplicate2 = list2.size() - receivedUniqueMessage2.size();
            duplicate = duplicate1 + duplicate2;
            System.out.println("receive messages length" + receivedMessage.size());
            System.out.println("unique size"+receivedUniqueMessage.size());
            messageRate = (double)receivedMessage.size()/time;
            duplicateRate = (double)duplicate/receivedMessage.size();
            System.out.println("message rate" + messageRate);
            System.out.println("duplicate rate"+duplicateRate);
            if (list1.size() != 0 & list2.size() != 0) {
                lossRate = (double)lossMessage(list1, duplicate1)[0]/lossMessage(list1,duplicate1)[1] + lossMessage(list2, duplicate2)[0]/lossMessage(list2,duplicate2)[1];
            }
            int ooo1 = oooMessage(receivedUniqueMessage1);
            int ooo2 = oooMessage(receivedUniqueMessage2);
            ooo = ooo1 + ooo2;
            oooRate = (double) ooo/receivedMessage.size();
            System.out.println("lose rate" + lossRate);
            System.out.println("ooo rate" + oooRate);
            meanGap = calculateGap(list1,list11,list2,list22)[0];
            sd = calculateGap(list1,list11,list2,list22)[1];
            System.out.println("the mean of inter gap message "+ meanGap);
            System.out.println("the standard deviation of inter gap message "+ sd);
        } else {
            /* calculate the receive rate for list doesn't need to split*/
            receivedUniqueMessage = removeDuplicate(receivedMessage);
            duplicate = receivedMessage.size() - receivedUniqueMessage.size();
            System.out.println("receive messages length" + receivedMessage.size());
            System.out.println("unique size" + receivedUniqueMessage.size());
            messageRate = (double) receivedMessage.size() / time;
            duplicateRate = (double) duplicate / receivedMessage.size();
            System.out.println("message rate" + messageRate);
            System.out.println("duplicate rate" + duplicateRate);
            if (receivedMessage.size() != 0) {
                lossRate = (double)lossMessage(receivedMessage, duplicate)[0]/lossMessage(receivedMessage,duplicate)[1];
            }
            ooo = oooMessage(receivedUniqueMessage);
            oooRate = (double) ooo / receivedMessage.size();
            System.out.println("lose rate" + lossRate);
            System.out.println("ooo rate" + oooRate);
            ArrayList<Integer> empty1 = new ArrayList<>();
            ArrayList<Long> empty2 = new ArrayList<>();
            meanGap = calculateGap(receivedMessage,receivedTime,empty1, empty2)[0];
            sd = calculateGap(receivedMessage,receivedTime,empty1,empty2)[1];
            System.out.println("the mean of inter gap message " + meanGap);
            System.out.println("the standard deviation of inter gap message " + sd);
        }
    }

    /*        If the messages are ...9999, 0, 1, ..., set into two lists: [0,1,....] and [....,9999]
        Assumption is made that if 0 meets and in later 10 seconds, it is stable (which means 1,2,3,4,5,6,7,8,9,10 or the ten numbers that all less than 100), it is acutally down to 0
        Otherwise, only consider it is oot or just a random number
        Since wrap is a very complex problem, I made an assumption in here
     */
    public dataType seperateList(ArrayList<Integer> messages, ArrayList<Long> times) {
        // get the index that 0 happens in the list
        ArrayList<Integer> appearList = allAppearance(messages, 0);
        // You might not understand why I initialize at here and some arraylist still need re-initialize later (It's for solving a list to arraylist problem)
        ArrayList<Integer> list1 = new ArrayList<>();
        ArrayList<Integer> list2 = new ArrayList<>();
        ArrayList<Long> list11 = new ArrayList<>();
        ArrayList<Long> list22 = new ArrayList<>();
        if (appearList.size() == 0) {
            list1 = messages;
        } else if (appearList.size() == 1) {
            if (check(messages, appearList.get(0))) {
                // can split from that index]
                list1 = new ArrayList<>(messages.subList(0,appearList.get(0)));
                list2 = new ArrayList<>(messages.subList(appearList.get(0), messages.size()));
                list11 = new ArrayList<>(times.subList(0, appearList.get(0)));
                list22 = new ArrayList<>(times.subList(appearList.get(0), messages.size()));
            } else {
                list1 = messages;
                list11 = times;
            }
        } else {
            for (int i = 0; i < appearList.size(); i++) {
                // if find one,add it
                if (check(messages, appearList.get(i))) {
                    // can split from that index
                    list1 = new ArrayList<>(messages.subList(0,appearList.get(0)));
                    list2 = new ArrayList<>(messages.subList(appearList.get(0), messages.size()));
                    list11 = new ArrayList<>(times.subList(0, appearList.get(0)));
                    list22 = new ArrayList<>(times.subList(appearList.get(0), messages.size()));
                } //if until the last one, still not suitable for check, dont split
                else if (i == appearList.size() -1 ) {
                    list1 = messages;
                    list11 = times;
                }
            }
        }
        // sublist right side is not included
//        ArrayList<Integer>[] result1 = new ArrayList[]{list1, list2};
//        ArrayList<Long>[] result2 = new ArrayList[]{list11,list22};
        //Pair<ArrayList<Integer>[],ArrayList<Long>[]> result = new Pair<>(result1, result2);
        dataType result = new dataType(list1, list2, list11, list22);
        return result;
    }

    public ArrayList<Integer> allAppearance(ArrayList<Integer> messages, int elem) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) == elem) {
                result.add(i);
            }
        }
        return result;
    }

    public Boolean check(ArrayList<Integer> messages, int index) {
        for (int i = 1; i < 11; i++) {
            if (messages.get(index+i) > 100) {
                return false;
            }
        }
        return true;
    }

    /* This method is used to calculate how many messages are lost. Assume the list element is sorted increasing
        1,2,3,3,5,7,8 -> duplicate = 1
        the total number should in there T = max - min + 1
        loss = T - (received_message - duplicate) = T -receive + dulicate = 8 - 7 + 1 = 2
        If the messages are ...9999, 0, 1, ... It should be cut into two lists for calculation

        input: messages is the receivedMessage(with duplication), dup is the number of duplication in the receivedMessage
     */
    public static int[] lossMessage(ArrayList<Integer> messages, int dup){
        Collections.sort(messages);
        int total = messages.get(messages.size()-1) - messages.get(0) + 1;
        int loss = total - messages.size() + dup;
        int[] result = new int[2];
        result[0] = loss;
        result[1] = total;
        return result;
    }

    /*
        The method- is used to calculate how many messages are out of order
        e.g. for the messages: 1,2,3,7,5,8 only 7 out of order
        check whether the current number is less than the next number, if not, (implssible equal at here), remove the current number
        e.g 1,2,3,4,9000,5 -> 1,2,3,4,5    9000 is out of order

        Note the input oooMessage is the message which is already filtered the duplication
     */
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

    /*
        This method is used to change the original received message list into the list without duplication and the previous order is still kept.
     */
    public static ArrayList<Integer> removeDuplicate(ArrayList<Integer> messgaes) {
        ArrayList<Integer> result = (ArrayList<Integer>) messgaes.stream().distinct().collect(Collectors.toList());
        return result;
    }

    /*
    This method is used to calculate the mean and standard deviation for the inter messgae gap. If meet the duplication or ooo, skip
    it. Say a sequence of messages, 1,2,3,3,5,4,6 -> (1,2),(2,3),(4,6)
     */
    public static double[] calculateGap(ArrayList<Integer> messages1, ArrayList<Long> times1, ArrayList<Integer> messages2, ArrayList<Long> times2) {
        // we don't need consider the last element in the list
        ArrayList<Long> gaps = new ArrayList<>();
        int count = 0;
        long totalGap = 0;
        if (messages2.size() != 0 & times2.size() != 0) {
        for (int i = 0; i < messages1.size() - 1; i++) {
            int current = messages1.get(i);
            int next = messages1.get(i+1);
            // if the next element is not duplicate and not ooo, it's ok to add the gap
            if (messages1.indexOf(next) == i+1 & current < next) {
                long current_time = times1.get(i);
                long next_time = times1.get(i+1);
                gaps.add(next_time - current_time);
                totalGap += (next_time - current_time);
                count += 1;
            } else {
                // if duplication/gap, skip the next
                i += 1;
            }
        }
        for (int i = 0; i < messages2.size() - 1; i++) {
            int current = messages2.get(i);
            int next = messages2.get(i+1);
            // if the next element is not duplicate and not ooo, it's ok to add the gap
            if (messages2.indexOf(next) == i+1 & current < next) {
                long current_time = times2.get(i);
                long next_time = times2.get(i+1);
                gaps.add(next_time - current_time);
                totalGap += (next_time - current_time);
                count += 1;
            } else {
                // if duplication/gap, skip the next
                i += 1;
            }
        }} else {
            for (int i = 0; i < messages1.size() - 1; i++) {
                int current = messages1.get(i);
                int next = messages1.get(i+1);
                // if the next element is not duplicate and not ooo, it's ok to add the gap
                if (messages1.indexOf(next) == i+1 & current < next) {
                    long current_time = times1.get(i);
                    long next_time = times1.get(i+1);
                    gaps.add(next_time - current_time);
                    totalGap += (next_time - current_time);
                    count += 1;
                } else {
                    // if duplication/gap, skip the next
                    i += 1;
                }
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


    public void doDemo(String subscribe_topic) throws InterruptedException {
        try {
            client = new MqttClient("tcp://comp3310.ddns.net", "3310-u6105656");
            int qos = Character.getNumericValue(subscribe_topic.split("/")[2].charAt(1));
            MqttConnectOptions m = new MqttConnectOptions();
            m.setCleanSession(true);
            m.setKeepAliveInterval(10);
            m.setUserName("students");
            m.setPassword("33106331".toCharArray());
            client.connect(m);
            client.setCallback(this);
            //client.subscribe("counter/fast/q1");
            client.subscribe(subscribe_topic,qos);
            Thread.sleep(1000*time);
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishResult(String subscribe_topic) throws MqttException {
        String channel = subscribe_topic.split("/")[1];
        int qos = Character.getNumericValue(subscribe_topic.split("/")[2].charAt(1));
        client = new MqttClient("tcp://comp3310.ddns.net", "3310-u6105656");
        MqttConnectOptions m = new MqttConnectOptions();
        m.setCleanSession(true);
        m.setKeepAliveInterval(10);
        m.setUserName("students");
        m.setPassword("33106331".toCharArray());
        client.connect(m);
        client.setCallback(this);
        MqttMessage language = new MqttMessage("Java".getBytes());
        MqttMessage network = new MqttMessage("WiFi".getBytes());
        MqttMessage recv = new MqttMessage((""+messageRate*100).getBytes());
        MqttMessage loss = new MqttMessage((""+lossRate*100).getBytes());
        MqttMessage dupe = new MqttMessage((""+duplicateRate*100).getBytes());
        MqttMessage ooo = new MqttMessage((""+oooRate*10).getBytes());
        MqttMessage gap = new MqttMessage((""+meanGap).getBytes());
        MqttMessage gvar = new MqttMessage((""+sd).getBytes());
        language.setQos(2);
        language.setRetained(true);
        network.setQos(2);
        network.setRetained(true);
        recv.setQos(2);
        recv.setRetained(true);
        loss.setQos(2);
        loss.setRetained(true);
        dupe.setQos(2);
        dupe.setRetained(true);
        ooo.setQos(2);
        ooo.setRetained(true);
        gap.setQos(2);
        gap.setRetained(true);
        gvar.setQos(2);
        gvar.setRetained(true);
        client.publish("studentreport/u6105656/language",language);
        client.publish("studentreport/u6105656/network",network);
        client.publish("studentreport/u6105656/"+channel+"/"+qos+"/recv",recv);
        client.publish("studentreport/u6105656/"+channel+"/"+qos+"/loss",loss);
        client.publish("studentreport/u6105656/"+channel+"/"+qos+"/dupe",dupe);
        client.publish("studentreport/u6105656/"+channel+"/"+qos+"/ooo", ooo);
        client.publish("studentreport/u6105656/"+channel+"/"+qos+"/gap", gap);
        client.publish("studentreport/u6105656/"+channel+"/"+qos+"/gvar",gvar);
        client.disconnect();
    }

    private static MqttConnectOptions setUpConnectionOptions(String username, String password) {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setUserName(username);
        connOpts.setPassword(password.toCharArray());
        return connOpts;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("The connection is lost");
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        System.out.println(mqttMessage);
        String message = mqttMessage.toString();
        // if received message is an integer
        long time = System.currentTimeMillis();
        //System.out.println("time "+ time);
        if (message.chars().allMatch(Character::isDigit)) {
            receivedMessage.add(Integer.parseInt(message));
            receivedTime.add(time);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        System.out.printf("The delivery is complete");
    }

}
