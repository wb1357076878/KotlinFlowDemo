package com.feiniu.mviwithflowdemo

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.feiniu.mviwithflowdemo.databinding.FragmentSecondBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        binding.buttonFlow.setOnClickListener {
            lifecycleScope.launch {
                val value = createFlow().first()
                Log.d("flow", "flow.first() = $value")

                val acc = createFlow().fold(0) { acc, item ->
                    acc + item
                }
                Log.d("flow", "flow.fold() = $acc")

                try {
                    val value = createFlow().single()
                    Log.d("flow", "flow.single() = $value")
                } catch (e: Exception) {
                    Log.d("flow", e.toString())
                }
            }
        }

        binding.collectLastBtn.setOnClickListener {
            lifecycleScope.launch {
                createFlow().collectLatest { value ->
                    println("Collecting $value")
                    delay(1000) // Emulate work
                    println("$value collected")
                }
            }
        }

        lifecycleScope.launch {
            createFlow().flowOn(Dispatchers.IO).collect {
                // flowOn 作用：将执行此流的上下文更改为给定的上下文。
            }
            createFlow().distinctUntilChanged().collectLatest {
                println("emit value = $it")
            }
        }
    }

    private fun createFlow(): Flow<Int> {
        return flow {
            emit(100)
            delay(500)
            emit(200)
//            emit(200)
            emit(300)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}