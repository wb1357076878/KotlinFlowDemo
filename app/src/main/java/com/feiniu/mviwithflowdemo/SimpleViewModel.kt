package com.feiniu.mviwithflowdemo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * @ClassName: SimpleViewModel
 * @Description: java类作用描述
 * @Author: WangBo
 * @Date: 2023/7/11 15:15
 */
class SimpleViewModel : ViewModel() {

    private val _liveData = MutableLiveData("Hello World")
    val liveData: LiveData<String> = _liveData

    private val _stateFlow = MutableStateFlow("Hello world")
    val stateFlow: StateFlow<String> = _stateFlow.asStateFlow()

    private val _sharedFlow = MutableSharedFlow<String>()
    val sharedFlow = _sharedFlow.asSharedFlow()

    fun triggerLiveData() {
        _liveData.value = "LiveData"
    }

    fun triggerFlow(): Flow<String> {
        return flow {
            repeat(5) {
                emit("count down ${5 - it}")
                delay(1000L)
            }
        }
    }

    fun triggerStateFlow() {
        _stateFlow.value = "StateFlow"
    }

    fun triggerSharedFlow() {
        viewModelScope.launch {
            _sharedFlow.emit("SharedFlow")
        }
    }

}