package com.nosenkomi.emotionclassification

import android.app.Application
import com.nosenkomi.emotionclassification.record.AndroidAudioRecorder
import com.nosenkomi.emotionclassification.record.AudioRecorder
import com.nosenkomi.emotionclassification.record.YamnetClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule{

    @Provides
    @Singleton
    fun provideAndroidAudioRecorder(application: Application): AudioRecorder {
        return AndroidAudioRecorder(application)
    }

    @Provides
    @Singleton
    fun provideYamnetClassifier(application: Application): YamnetClassifier {
        return YamnetClassifier(application)
    }

    @Provides
    @Singleton
    fun provideMainActivityViewModel(recorder: AudioRecorder, classifier: YamnetClassifier): MainActivityViewModel{
        return MainActivityViewModel(recorder, classifier)
    }
}