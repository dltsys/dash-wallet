/*
 * Copyright 2011-2013 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package hashengineering.digitalcoin.wallet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.digitalcoin.core.CoinDefinition;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.DateUtils;

import com.google.digitalcoin.core.Utils;

import hashengineering.digitalcoin.wallet.util.GenericUtils;
import hashengineering.digitalcoin.wallet.util.Io;

/**
 * @author Andreas Schildbach
 */
public class ExchangeRatesProvider extends ContentProvider
{
	public static class ExchangeRate
	{
		public ExchangeRate(@Nonnull final String currencyCode, @Nonnull final BigInteger rate, @Nonnull final String source)
		{
			this.currencyCode = currencyCode;
			this.rate = rate;
			this.source = source;
		}

		public final String currencyCode;
		public final BigInteger rate;
		public final String source;

		@Override
		public String toString()
		{
			return getClass().getSimpleName() + '[' + currencyCode + ':' + GenericUtils.formatValue(rate, Constants.BTC_MAX_PRECISION) + ']';
		}
	}

	public static final String KEY_CURRENCY_CODE = "currency_code";
	private static final String KEY_RATE = "rate";
	private static final String KEY_SOURCE = "source";

	@CheckForNull
	private Map<String, ExchangeRate> exchangeRates = null;
	private long lastUpdated = 0;

	private static final long UPDATE_FREQ_MS = 10 * DateUtils.MINUTE_IN_MILLIS;

	private static final Logger log = LoggerFactory.getLogger(ExchangeRatesProvider.class);

	@Override
	public boolean onCreate()
	{
		return true;
	}

	public static Uri contentUri(@Nonnull final String packageName)
	{
		return Uri.parse("content://" + packageName + '.' + "exchange_rates");
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder)
	{
		final long now = System.currentTimeMillis();

		if (exchangeRates == null || now - lastUpdated > UPDATE_FREQ_MS)
		{
			Map<String, ExchangeRate> newExchangeRates = getBitcoinCharts();
			if (exchangeRates == null && newExchangeRates == null)
				newExchangeRates = getBlockchainInfo();

			if (newExchangeRates != null)
			{
				exchangeRates = newExchangeRates;
				lastUpdated = now;
			}
		}

		if (exchangeRates == null)
			return null;

		final MatrixCursor cursor = new MatrixCursor(new String[] { BaseColumns._ID, KEY_CURRENCY_CODE, KEY_RATE, KEY_SOURCE });

		if (selection == null)
		{
			for (final Map.Entry<String, ExchangeRate> entry : exchangeRates.entrySet())
			{
				final ExchangeRate rate = entry.getValue();
				cursor.newRow().add(rate.currencyCode.hashCode()).add(rate.currencyCode).add(rate.rate.longValue()).add(rate.source);
			}
		}
		else if (selection.equals(KEY_CURRENCY_CODE))
		{
			final String selectedCode = selectionArgs[0];
			ExchangeRate rate = selectedCode != null ? exchangeRates.get(selectedCode) : null;

			if (rate == null)
			{
				final String defaultCode = defaultCurrencyCode();
				rate = defaultCode != null ? exchangeRates.get(defaultCode) : null;

				if (rate == null)
				{
					rate = exchangeRates.get(Constants.DEFAULT_EXCHANGE_CURRENCY);

					if (rate == null)
						return null;
				}
			}

			cursor.newRow().add(rate.currencyCode.hashCode()).add(rate.currencyCode).add(rate.rate.longValue()).add(rate.source);
		}

		return cursor;
	}

	private String defaultCurrencyCode()
	{
		try
		{
			return Currency.getInstance(Locale.getDefault()).getCurrencyCode();
		}
		catch (final IllegalArgumentException x)
		{
			return null;
		}
	}

	public static ExchangeRate getExchangeRate(@Nonnull final Cursor cursor)
	{
		final String currencyCode = cursor.getString(cursor.getColumnIndexOrThrow(ExchangeRatesProvider.KEY_CURRENCY_CODE));
		final BigInteger rate = BigInteger.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(ExchangeRatesProvider.KEY_RATE)));
		final String source = cursor.getString(cursor.getColumnIndexOrThrow(ExchangeRatesProvider.KEY_SOURCE));

		return new ExchangeRate(currencyCode, rate, source);
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(final Uri uri, final String selection, final String[] selectionArgs)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType(final Uri uri)
	{
		throw new UnsupportedOperationException();
	}

    private static Object getCoinValueBTC()
    {
        Date date = new Date();
        long now = date.getTime();



        //final Map<String, ExchangeRate> rates = new TreeMap<String, ExchangeRate>();
        // Keep the LTC rate around for a bit
        Double btcRate = 0.0;
        String currencyCryptsy = CoinDefinition.cryptsyMarketCurrency;
        String urlCryptsy = "http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid="+ CoinDefinition.cryptsyMarketId;




        try {
            // final String currencyCode = currencies[i];
            final URL URLCryptsy = new URL(urlCryptsy);
            final URLConnection connectionCryptsy = URLCryptsy.openConnection();
            connectionCryptsy.setConnectTimeout(Constants.HTTP_TIMEOUT_MS * 2);
            connectionCryptsy.setReadTimeout(Constants.HTTP_TIMEOUT_MS * 2);
            connectionCryptsy.connect();

            final StringBuilder contentCryptsy = new StringBuilder();

            Reader reader = null;
            try
            {
                reader = new InputStreamReader(new BufferedInputStream(connectionCryptsy.getInputStream(), 1024));
                Io.copy(reader, contentCryptsy);
                final JSONObject head = new JSONObject(contentCryptsy.toString());
                JSONObject returnObject = head.getJSONObject("return");
                JSONObject markets = returnObject.getJSONObject("markets");
                JSONObject coinInfo = markets.getJSONObject(CoinDefinition.coinTicker);



                JSONArray recenttrades = coinInfo.getJSONArray("recenttrades");

                double btcTraded = 0.0;
                double coinTraded = 0.0;

                for(int i = 0; i < recenttrades.length(); ++i)
                {
                    JSONObject trade = (JSONObject)recenttrades.get(i);

                    btcTraded += trade.getDouble("total");
                    coinTraded += trade.getDouble("quantity");

                }

                Double averageTrade = btcTraded / coinTraded;



                //Double lastTrade = GLD.getDouble("lasttradeprice");



                //String euros = String.format("%.7f", averageTrade);
                // Fix things like 3,1250
                //euros = euros.replace(",", ".");
                //rates.put(currencyCryptsy, new ExchangeRate(currencyCryptsy, Utils.toNanoCoins(euros), URLCryptsy.getHost()));
                if(currencyCryptsy.equalsIgnoreCase("BTC")) btcRate = averageTrade;

            }
            finally
            {
                if (reader != null)
                    reader.close();
            }
            return btcRate;
        }
        catch (final IOException x)
        {
            x.printStackTrace();
        }
        catch (final JSONException x)
        {
            x.printStackTrace();
        }

        return null;
    }

	private static Map<String, ExchangeRate> getBitcoinCharts()
	{
		try
		{
            Double btcRate = 0.0;

            Object result = getCoinValueBTC();

            if(result == null)
                return null;

            else btcRate = (Double)result;

			final URL URL = new URL("http://api.bitcoincharts.com/v1/weighted_prices.json");
			final HttpURLConnection connection = (HttpURLConnection) URL.openConnection();
			connection.setConnectTimeout(Constants.HTTP_TIMEOUT_MS);
			connection.setReadTimeout(Constants.HTTP_TIMEOUT_MS);
			connection.connect();

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				return null;

			Reader reader = null;
			try
			{
				reader = new InputStreamReader(new BufferedInputStream(connection.getInputStream(), 1024), Constants.UTF_8);
				final StringBuilder content = new StringBuilder();
				Io.copy(reader, content);

				final Map<String, ExchangeRate> rates = new TreeMap<String, ExchangeRate>();

                //Add Bitcoin information
                rates.put(CoinDefinition.cryptsyMarketCurrency, new ExchangeRate(CoinDefinition.cryptsyMarketCurrency, Utils.toNanoCoins(String.format("%.8f", btcRate).replace(",", ".")), "pubapi.cryptsy.com"));

				final JSONObject head = new JSONObject(content.toString());
				for (final Iterator<String> i = head.keys(); i.hasNext();)
				{
					final String currencyCode = i.next();
					if (!"timestamp".equals(currencyCode))
					{
						final JSONObject o = head.getJSONObject(currencyCode);
						String rate = o.optString("24h", null);
						if (rate == null)
							rate = o.optString("7d", null);
						if (rate == null)
							rate = o.optString("30d", null);

                        double rateForBTC = Double.parseDouble(rate);

                        rate = String.format("%.8f", rateForBTC * btcRate);

						if (rate != null)
						{
							try
							{
								rates.put(currencyCode, new ExchangeRate(currencyCode, Utils.toNanoCoins(rate.replace(",", ".")), URL.getHost()));
							}
							catch (final ArithmeticException x)
							{
								log.debug("problem reading exchange rate: " + currencyCode, x);
							}
						}
					}
				}

				return rates;
			}
			finally
			{
				if (reader != null)
					reader.close();
			}
		}
		catch (final Exception x)
		{
			log.debug("problem reading exchange rates", x);
		}

		return null;
	}

	private static Map<String, ExchangeRate> getBlockchainInfo()
	{
		try
		{
            Double btcRate = 0.0;

            Object result = getCoinValueBTC();

            if(result == null)
                return null;

            else btcRate = (Double)result;

			final URL URL = new URL("https://blockchain.info/ticker");
			final HttpURLConnection connection = (HttpURLConnection) URL.openConnection();
			connection.setConnectTimeout(Constants.HTTP_TIMEOUT_MS);
			connection.setReadTimeout(Constants.HTTP_TIMEOUT_MS);
			connection.connect();

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				return null;

			Reader reader = null;
			try
			{
				reader = new InputStreamReader(new BufferedInputStream(connection.getInputStream(), 1024), Constants.UTF_8);
				final StringBuilder content = new StringBuilder();
				Io.copy(reader, content);

				final Map<String, ExchangeRate> rates = new TreeMap<String, ExchangeRate>();

                //Add Bitcoin information
                rates.put(CoinDefinition.cryptsyMarketCurrency, new ExchangeRate(CoinDefinition.cryptsyMarketCurrency, Utils.toNanoCoins(String.format("%.8f", btcRate).replace(",", ".")), "pubapi.cryptsy.com"));

                final JSONObject head = new JSONObject(content.toString());
				for (final Iterator<String> i = head.keys(); i.hasNext();)
				{
					final String currencyCode = i.next();
					final JSONObject o = head.getJSONObject(currencyCode);
					//final String rate = o.optString("15m", null);

                    double rateForBTC = o.getDouble("15m") * btcRate;

                    final String rate = String.format("%.8f",rateForBTC); //o.optString("15m", null);

					if (rate != null)
					{
						try
						{
							rates.put(currencyCode, new ExchangeRate(currencyCode, Utils.toNanoCoins(rate.replace(",", ".")), URL.getHost()));
						}
						catch (final ArithmeticException x)
						{
							log.debug("problem reading exchange rate: " + currencyCode, x);
						}
					}
				}

				return rates;
			}
			finally
			{
				if (reader != null)
					reader.close();
			}
		}
		catch (final Exception x)
		{
			log.debug("problem reading exchange rates", x);
		}

		return null;
	}

	// https://bitmarket.eu/api/ticker
}