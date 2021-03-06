package com.example.viewmodel;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.example.StocksConfig;
import com.example.activity.StockDetailActivity;
import com.example.entity.QuoteEntity;
import com.example.rest.RestHttpLogger;
import com.example.rest.RestResponseHandler;
import com.example.rest.provider.StocksServiceProvider;

import org.alfonz.rest.HttpException;
import org.alfonz.rest.call.CallManager;
import org.alfonz.rest.call.Callback;
import org.alfonz.utility.NetworkUtility;
import org.alfonz.view.StatefulLayout;

import retrofit2.Call;
import retrofit2.Response;


public class StockDetailViewModel extends BaseViewModel implements LifecycleObserver
{
	public final ObservableField<Integer> state = new ObservableField<>();
	public final ObservableField<QuoteEntity> quote = new ObservableField<>();

	private String mSymbol;
	private CallManager mCallManager = new CallManager(new RestResponseHandler(), new RestHttpLogger());


	public StockDetailViewModel(Bundle extras)
	{
		// handle intent extras
		handleExtras(extras);
	}


	@OnLifecycleEvent(Lifecycle.Event.ON_START)
	public void onStart()
	{
		// load data
		if(quote.get() == null) loadData();
	}


	@Override
	public void onCleared()
	{
		super.onCleared();

		// cancel async tasks
		if(mCallManager != null) mCallManager.cancelRunningCalls();
	}


	public void loadData()
	{
		sendQuote(mSymbol);
	}


	public void refreshData()
	{
		sendQuote(mSymbol);
	}


	public String getChartUrl()
	{
		return String.format(StocksConfig.CHART_BASE_URL, mSymbol);
	}


	private void sendQuote(String symbol)
	{
		if(NetworkUtility.isOnline(getApplicationContext()))
		{
			String callType = StocksServiceProvider.QUOTE_CALL_TYPE;
			if(!mCallManager.hasRunningCall(callType))
			{
				// show progress
				state.set(StatefulLayout.PROGRESS);

				// enqueue call
				Call<QuoteEntity> call = StocksServiceProvider.getService().quote("json", symbol);
				QuoteCallback callback = new QuoteCallback(mCallManager);
				mCallManager.enqueueCall(call, callback, callType);
			}
		}
		else
		{
			// show offline
			state.set(StatefulLayout.OFFLINE);
		}
	}


	private void setState(ObservableField<QuoteEntity> data)
	{
		if(data.get() != null)
		{
			state.set(StatefulLayout.CONTENT);
		}
		else
		{
			state.set(StatefulLayout.EMPTY);
		}
	}


	private void handleExtras(Bundle extras)
	{
		if(extras != null)
		{
			mSymbol = extras.getString(StockDetailActivity.EXTRA_SYMBOL);
		}
	}


	private class QuoteCallback extends Callback<QuoteEntity>
	{
		public QuoteCallback(CallManager callManager)
		{
			super(callManager);
		}


		@Override
		public void onSuccess(@NonNull Call<QuoteEntity> call, @NonNull Response<QuoteEntity> response)
		{
			quote.set(response.body());
			setState(quote);
		}


		@Override
		public void onError(@NonNull Call<QuoteEntity> call, @NonNull HttpException exception)
		{
			handleError(mCallManager.getHttpErrorMessage(exception));
			setState(quote);
		}


		@Override
		public void onFail(@NonNull Call<QuoteEntity> call, @NonNull Throwable throwable)
		{
			handleError(mCallManager.getHttpErrorMessage(throwable));
			setState(quote);
		}
	}
}
