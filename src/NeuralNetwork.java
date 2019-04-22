import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.NumberedFileInputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.layers.recurrent.Bidirectional;
import org.deeplearning4j.nn.conf.layers.recurrent.SimpleRnn;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.*;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.schedule.ExponentialSchedule;
import org.nd4j.linalg.schedule.ScheduleType;
import org.nd4j.linalg.schedule.SigmoidSchedule;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class NeuralNetwork {

    //FeedForward Netzwerk
    public static void feedForwardTest (String trainPath, String testPath, int nrDimension, int batchsize, int nrEpochs) throws IOException, InterruptedException {

        final String fileNameTrain = trainPath;
        final String fileNameTest = testPath;

        //TrainIterator
        RecordReader rrTrain = new CSVRecordReader();
        rrTrain.initialize(new FileSplit(new File(fileNameTrain)));
        DataSetIterator trainIter = new RecordReaderDataSetIterator(rrTrain,batchsize,0,17);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                //.biasInit(0.9)
                .updater(new Sgd(0.007))
                //.updater(new Nesterovs(0.005,0.9))
                .list()
                .layer(new DenseLayer.Builder().nIn(nrDimension).nOut(nrDimension*2)
                        .weightInit(WeightInit.RELU)
                        .activation(Activation.TANH)
                        .build())
                .layer(new DenseLayer.Builder().nIn(nrDimension*2).nOut(nrDimension*2)
                        .weightInit(WeightInit.RELU)
                        .activation(Activation.TANH)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .weightInit(WeightInit.RELU)
                        .activation(Activation.SOFTMAX)
                        .nIn(nrDimension*2).nOut(17).build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        for (int i=0;i<nrEpochs;i++) {
            model.fit(trainIter);
        }

        RecordReader rrTest = new CSVRecordReader();
        rrTest.initialize(new FileSplit(new File(fileNameTest)));
        DataSetIterator testIter = new RecordReaderDataSetIterator(rrTest,batchsize,0,17);

        Evaluation eval = new Evaluation(17);

        while(testIter.hasNext()) {
            DataSet t = testIter.next();
            INDArray features = t.getFeatures();
            INDArray labels = t.getLabels();
            INDArray predicted = model.output(features,false);

            eval.eval(labels,predicted);
        }
        System.out.print(eval.stats());

    }
    //Recurrentes LSTM Netzwerk
    public static void recurrentTest (int batchsize,double learningrate,double bias, double forgetBias, int backprop, int dimensions, int nrEpochs, String absolutePath) throws IOException, InterruptedException {
        Date date = new Date();
        System.out.println(date.toString());

        SequenceRecordReader testreader = new CSVSequenceRecordReader(0, ",");
        SequenceRecordReader testLabelreader = new CSVSequenceRecordReader(0, ",");
        testreader.initialize(new NumberedFileInputSplit(absolutePath + "/Test/%dDim_"+ dimensions +".csv", 0, 1190));
        testLabelreader.initialize(new NumberedFileInputSplit(absolutePath + "/TestRead/LABEL%d.csv", 0, 1190));

        DataSetIterator test = new SequenceRecordReaderDataSetIterator(testreader,testLabelreader, batchsize, 17,  false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END );

        SequenceRecordReader trainreader = new CSVSequenceRecordReader(0, ",");
        SequenceRecordReader trainLabelreader = new CSVSequenceRecordReader(0, ",");
        trainreader.initialize(new NumberedFileInputSplit(absolutePath + "/Train/%dDim_"+ dimensions +".csv", 0, 1242));
        trainLabelreader.initialize(new NumberedFileInputSplit(absolutePath + "/TrainRead/LABEL%d.csv", 0, 1242));

        Layer bilstm = new LSTM.Builder().nIn(dimensions).nOut(dimensions)
                .activation(Activation.HARDTANH)
                .forgetGateBiasInit(forgetBias)
                .gateActivationFunction(Activation.SIGMOID)
                .build();

        DataSetIterator train = new SequenceRecordReaderDataSetIterator(trainreader,trainLabelreader, batchsize, 17, false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                //.updater(new AdaGrad(new SigmoidSchedule(ScheduleType.ITERATION,0.005,0.9, (int) 0.01)))
                //.updater(new Adam(new ExponentialSchedule(ScheduleType.ITERATION,0.005,0.9)))
                //.updater(new Nesterovs(0.001,0.9))
                .updater(new Sgd(learningrate))
                .weightInit(WeightInit.XAVIER)
                .biasInit(bias)
                //.gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue).gradientNormalizationThreshold(1.0)
                .list()
                .layer(0,new LSTM.Builder().nIn(dimensions).nOut(dimensions)
                    .activation(Activation.HARDTANH)
                    .forgetGateBiasInit(forgetBias)
                    .gateActivationFunction(Activation.SIGMOID)
                    .build())
                //.layer(0, new Bidirectional(Bidirectional.Mode.AVERAGE,bilstm))
                .layer(1, new RnnOutputLayer.Builder().activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nIn(dimensions).nOut(17).build())
                .backpropType(BackpropType.TruncatedBPTT).tBPTTForwardLength(backprop).tBPTTBackwardLength(backprop)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        for (int i = 0;i<nrEpochs;i++) {
            net.fit(train);
        }

        Evaluation eval = net.evaluate(test);
        System.out.println("Parameter: " + "\n" +"Batchsize: " + batchsize + "Lernrate:" + learningrate+ "Bias: " + bias + "ForgetBias: " + forgetBias + "Backproprange: " + backprop);
        System.out.println(eval.stats());
    }

}
