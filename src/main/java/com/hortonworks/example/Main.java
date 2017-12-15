package com.hortonworks.example;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import org.apache.commons.math3.distribution.UniformRealDistribution;




/**
 * TODO
 * add functions
 * add a start and stop timer for benchmarking
 * add kryo serializer
 */
public class Main implements Serializable{

    private static JavaSparkContext sc = null;

    private static SQLContext sqlContext = null;

    final int NUM_TRIALS = 1;
    JavaPairRDD<String, String> symbolsAndWeightsRDD = null;
    Map<String, Float> symbolsAndWeights;
    Float totalInvestement;


    public static void main(String[] args) throws Exception {
        Main m = new Main();
        String listOfCompanies = new File("companies_list.txt").toURI().toString();
        String stockDataDir = "hdfs://sandbox.hortonworks.com/tmp/stockData/*.csv";
        if (sc == null) {
            SparkConf conf = new SparkConf().setAppName("monte-carlo-var-calculator");
            sc = new JavaSparkContext(conf);
            sqlContext = new org.apache.spark.sql.SQLContext(sc);
        }

        if (args.length > 0) {
            listOfCompanies = args[0];
        }
        if (args.length > 1) {
            stockDataDir = args[1];
        }
        JavaRDD<String> filteredFileRDD = sc.textFile(listOfCompanies).filter(s -> !s.startsWith("#") && !s.trim().isEmpty());
        m.run(args,filteredFileRDD,stockDataDir);
        m.close();
    }

    void close() {
        sc.stop();
    }

    void redistribute(JavaRDD<String> filteredFileRDD,float amt)
    {
        Set<Integer> arl = new HashSet<Integer>();

//        Random r = new Random();
////        TODO make sure there are no duplicates
//        while(arl.size()<4){
//            arl.add(r.nextInt((10 - 1) + 1) + 1);
//        }


        UniformRealDistribution urd = new UniformRealDistribution(0,11);


        while(arl.size()<4){
            arl.add((int)urd.sample());
        }


        float w =0.25f;
        symbolsAndWeightsRDD = filteredFileRDD.filter(s -> !s.startsWith("Symbol")).mapToPair(s ->
        {

            String[] splits = s.split(",", -2);


            if (arl.contains(Integer.parseInt(splits[0])))
                return new Tuple2<>(splits[1], "$"+(amt*0.25));
            else
                return new Tuple2<>(splits[1], "$0");
        });

        symbolsAndWeightsRDD.take(10).forEach(x -> System.out.println(x._1() + "->" + x._2()));


        if (symbolsAndWeightsRDD.first()._2().contains("$")) {
            JavaPairRDD<String, Float> symbolsAndDollarsRDD = symbolsAndWeightsRDD.mapToPair(x -> new Tuple2<>(x._1(), new Float(x._2().replaceAll("\\$", ""))));

            totalInvestement = symbolsAndDollarsRDD.reduce((x, y) -> new Tuple2<>("total", x._2()+y._2()))._2().floatValue();

//            totalInvestement = symbolsAndDollarsRDD.reduce((x, y) -> new Tuple2<>("total", x._2() + y._2()))._2().longValue();

            symbolsAndWeights = symbolsAndDollarsRDD.mapToPair(x -> new Tuple2<>(x._1(), (x._2() / totalInvestement))).collectAsMap();
            symbolsAndWeights.forEach((s, f) -> System.out.println("symbol: " + s + ", % of portfolio: " + f));
        } else {
            totalInvestement = 1000.0f;
            symbolsAndWeights = symbolsAndWeightsRDD.mapToPair(x -> new Tuple2<>(x._1(), new Float(x._2()))).collectAsMap();
        }

        //debug
        System.out.println("symbolsAndWeights");
        System.out.println("TOTAL=============="+totalInvestement);
        symbolsAndWeights.forEach((s, f) -> System.out.println("symbol: " + s + ", % of portfolio: " + f));


    }

    Object run(String[] args, JavaRDD<String> filteredFileRDD,String stockDataDir) {
   /*
   Initializations
   */




   /*
   read a list of stock symbols and their weights in the portfolio, then transform into a Map<Symbol,Weight>
   1. read in the data, ignoring header
   2. convert dollar amounts to fractions
   3. create a local map
   */
        float amt= 1000.0f;


        redistribute(filteredFileRDD,amt);
        //convert from $ to % weight in portfolio

   /*
   read all stock trading data, and transform
   1. get a PairRDD of date -> (symbol, changeInPrice)
   2. reduce by key to get all dates together
   3. filter every date that doesn't have the max number of symbols
\        */

        // 1. get a PairRDD of date -> Tuple2(symbol, changeInPrice)




        JavaPairRDD<String, Tuple2> datesToSymbolsAndChangeRDD = sc.textFile(stockDataDir).filter(s -> !s.contains("Change_Pct")).mapToPair(x -> {
            //skip header
//            if (x.contains("Change_Pct")) {
////                return Collections.EMPTY_LIST;
//                return Collections.singletonList(new Tuple2<>("2017-12-01", new Tuple2<>("GE", 0.0)));
//
//            }
            String[] splits = x.split(",", -2);

            Float changeInPrice = new Float(splits[8]);
            String symbol = splits[7];
            String date = splits[0];

//            return Collections.singletonList(new Tuple2<>(date, new Tuple2<>(symbol, changeInPrice)));
            return new Tuple2<>(date, new Tuple2<>(symbol, changeInPrice));
        });



//        //debug
//        datesToSymbolsAndChangeRDD.take(10).forEach(x -> System.out.println(x._1() + "->" + x._2()));
//
//        //2. reduce by key to get all dates together
        JavaPairRDD<String, Iterable<Tuple2>> groupedDatesToSymbolsAndChangeRDD = datesToSymbolsAndChangeRDD.groupByKey();
//        //debug

//        //3. filter every date that doesn't have the max number of symbols
        long numSymbols = symbolsAndWeightsRDD.count();
//        Map<String, Object> countsByDate = datesToSymbolsAndChangeRDD.countByKey();
        JavaPairRDD<String, Iterable<Tuple2>> filterdDatesToSymbolsAndChangeRDD = groupedDatesToSymbolsAndChangeRDD;
//        JavaPairRDD<String, Iterable<Tuple2>> filterdDatesToSymbolsAndChangeRDD = groupedDatesToSymbolsAndChangeRDD.filter(x -> (Long) countsByDate.get(x._1()) >= numSymbols);
        long numEvents = filterdDatesToSymbolsAndChangeRDD.count();
        //debug
        System.out.println("num symbols: " + numSymbols);
//        filterdDatesToSymbolsAndChangeRDD.take(10).forEach(x -> System.out.println(x._1() + "->" + x._2()));



   /*
   execute NUM_TRIALS
   1. pick a random date from the list of historical trade dates
   2. sum(stock weight in overall portfolio * change in price on that date)
    */
        double fraction = 1.0 * NUM_TRIALS / numEvents;
        Float total;


        Map<String, Iterable<Tuple2>> newmap = new TreeMap<String, Iterable<Tuple2>>();
        Map<String, Float> trialsMap = new TreeMap<String, Float>();
        groupedDatesToSymbolsAndChangeRDD.take(10).forEach(i ->
                newmap.put(i._1(), i._2())
        );

//        JavaPairRDD<String, Float> resultOfTrials = groupedDatesToSymbolsAndChangeRDD.mapToPair(i -> {
          for (String k: newmap.keySet()) {
              total = 0f;

              Float compChange = 0.0f;
              for (Tuple2 t : newmap.get(k)) {
                  String symbol = t._1().toString();
                  Float changeInPrice = new Float(t._2().toString());
                  Float weight = symbolsAndWeights.get(symbol);
                  compChange += changeInPrice;
                  total += changeInPrice * weight;
                  //debug
                  System.out.println("on " + k + " " + symbol + " with weight " + weight + " changed by " + changeInPrice
                          + " for a total of " + total);
              }
              if (total < 1.2) {
                  redistribute(filteredFileRDD,amt*((total/100.0f)+1));
              }


              trialsMap.put(k, total);
          }


        List<Tuple2<String,Float>> list = new ArrayList<>();
        for(Map.Entry<String,Float> entry : trialsMap.entrySet()){
            list.add(new Tuple2<>(entry.getKey(),entry.getValue()));
        }

        JavaPairRDD<String, Float> resultOfTrials = sc.parallelizePairs(list);

//        //debug
        System.out.println("fraction: " + fraction);
        System.out.println("total runs: " + resultOfTrials.count());
        resultOfTrials.take(10).forEach(System.out::println);

   /*
   create a temporary table out of the data and take the 5%, 50%, and 95% percentiles

   1. multiple each float by 100
   2. create an RDD with Row types
   3. Create a schema
   4. Use that schema to create a data frame
   5. execute Hive percentile() SQL function
    */
        JavaRDD<Row> resultOfTrialsRows = resultOfTrials.map(x -> RowFactory.create(x._1(), Math.round(x._2() * 100)));
        resultOfTrialsRows.take(10).forEach(x -> System.out.println(x));
        StructType schema = DataTypes.createStructType(new StructField[]{DataTypes.createStructField("date", DataTypes.StringType, false), DataTypes.createStructField("changePct", DataTypes.IntegerType, false)});
//        DataFrame resultOfTrialsDF = sqlContext.createDataFrame(resultOfTrialsRows, schema);
        Dataset<Row> resultOfTrialsDF = sqlContext.createDataFrame(resultOfTrialsRows, schema);

        resultOfTrialsDF.registerTempTable("results");
        List<Row> percentilesRow = sqlContext.sql("select percentile(changePct, array(0.05,0.50,0.95)) from results").collectAsList();

        System.out.println(sqlContext.sql("select * from results order by changePct").collectAsList());
        float worstCase = new Float(percentilesRow.get(0).getList(0).get(0).toString()) / 100;
        float mostLikely = new Float(percentilesRow.get(0).getList(0).get(1).toString()) / 100;
        float bestCase = new Float(percentilesRow.get(0).getList(0).get(2).toString()) / 100;

        System.out.println("Over the time period, this is what could happen to your stock holdings if you have $" + amt + " invested");
        System.out.println(String.format("%25s %7s %7s", "", "$", "%"));
        System.out.println(String.format("%25s %7d %7.2f%%", "worst case", Math.round(amt * worstCase / 100), worstCase));
        System.out.println(String.format("%25s %7d %7.2f%%", "most likely scenario", Math.round(amt * mostLikely / 100), mostLikely));
        System.out.println(String.format("%25s %7d %7.2f%%", "best case", Math.round(amt * bestCase / 100), bestCase));

//        return worstCase;

        resultOfTrialsRows.saveAsTextFile(args[2]);

        return 0.0;
    }

}