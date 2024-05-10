package com.nosenkomi.emotionclassification

import android.app.Application
import com.nosenkomi.emotionclassification.classifier.Classifier
import com.nosenkomi.emotionclassification.classifier.CnnLstmClassifier
import com.nosenkomi.emotionclassification.classifier.EmotionClassifier
import com.nosenkomi.emotionclassification.mlmodel.CnnGruModel
import com.nosenkomi.emotionclassification.mlmodel.MLModel
import com.nosenkomi.emotionclassification.mlmodel.YamnetModel
import com.nosenkomi.emotionclassification.record.AndroidAudioRecorder
import com.nosenkomi.emotionclassification.record.AudioRecorder
import com.nosenkomi.emotionclassification.record.CustomClassifier
import com.nosenkomi.emotionclassification.record.YamnetClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAndroidAudioRecorder(application: Application): AudioRecorder {
        return AndroidAudioRecorder(application, sampleRate = 16000)
    }

    @Provides
    @Singleton
    fun provideYamnetClassifier(application: Application): YamnetClassifier {
        return YamnetClassifier(application)
    }

    @Provides
    @Singleton
    fun provideAudioClassifier(application: Application): CustomClassifier {
        return CustomClassifier(application)
    }

    @Provides
    @Singleton
    fun provideLSTMClassifier(application: Application): CnnLstmClassifier {
        return CnnLstmClassifier(application)
    }

    @Provides
    @Singleton
    fun provideBufferMLClassifier(application: Application): MLModel<TensorBuffer> {
        return CnnGruModel(application)
    }

    @Provides
    @Singleton
    fun provideAudioMLClassifier(application: Application): MLModel<TensorAudio> {
        return YamnetModel(application)
    }

    @Provides
    @Singleton
    fun provideEmotionClassifier(
        audioRecorder: AudioRecorder,
        serModel: MLModel<TensorBuffer>,
        yamnetModel: MLModel<TensorAudio>
    ): Classifier {
        return EmotionClassifier(
            audioRecorder = audioRecorder,
            serModel = serModel,
            yamnetModel = yamnetModel,
        )
    }


//    @Provides
//    @Singleton
//    fun provideMainActivityViewModel(recorder: AudioRecorder, classifier: CnnLstmClassifier): MainActivityViewModel{
//        return MainActivityViewModel(recorder, classifier)
//    }

    @Provides
    @Singleton
    fun provideMainActivityViewModel(
        recorder: AudioRecorder,
        classifier: EmotionClassifier
    ): MainActivityViewModel {
        return MainActivityViewModel(recorder, classifier)
    }
}