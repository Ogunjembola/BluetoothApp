package com.appevolutionng.bluetooth.ui

interface ListInteractionListener<T>{


    fun onItemClick(item: T)

    fun startLoading()


    fun endLoading(partialResults: Boolean)


    fun endLoadingWithDialog(error: Boolean, element: T)

}
