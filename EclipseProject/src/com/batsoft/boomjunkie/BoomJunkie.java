package com.batsoft.boomjunkie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class BoomJunkie extends Activity {
	private final int AD_LOAD_DELAY = Settings.AD_SHOW_DELAY * 1000;
	public int bpm = 120, sequencerTickCount = 16;
	public int tickInterval = 15000 / bpm;
	public int drumCount = 6;
	public final int maxDrumCount = 15;
	public int screenWidth, screenHeight;
	public ArrayList<ArrayList<SequencerTickButton>> btnSequencerBarTicks;
	public ArrayList<DrumButton> btnDrums;
	public ArrayList<LinearLayout> layoutsSequencerTicks;
	public SoundPool drumPlayer;
	public int[] soundIDs;
	public int currentClickedDrumButton;
	public boolean isLoop = true;
	public Thread playerThread;

	// Views
	public CheckBox checkLoop;
	public ScrollView vScroll;
	public HorizontalScrollView hScroll;
	public ImageButton btnStop, btnPlay;
	public LinearLayout layoutSequencerTicks, layoutDrums;
	public SeekBar seekLength, seekBPM;

	public int buttonSideLength;
	public TextView textLength, textBPM;
	public String filename;
	public SaveData saveData;
	public File[] subFiles;
	private InterstitialAd interstitial;
	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_boom_junkie);
		currentClickedDrumButton = 0;

		initializeScreenSize();
		initializeViews();
		initializeSounds();
		loadDefaultBeat();
		initializeAds();
	}

	public void loadDefaultBeat() {
		try {
			AssetManager assetMgr = getAssets();
			InputStream fis = assetMgr.open("starting");
			HackedObjectInputStream is = new HackedObjectInputStream(fis);
			saveData = (SaveData) is.readObject();
			is.close();
			fis.close();
			interpretSaveData();

		} catch (Exception ioe) {
			Log.e("Error", "IOException - Beginning", ioe);
		}
	}

	@Override
	public void onStop() {
		stop(null);
		handler.removeCallbacksAndMessages(null);
		super.onStop();
	}

	public void initializeAds() {
		if (handler == null)
			handler = new Handler();
		if (interstitial == null)
			interstitial = new InterstitialAd(this);
		interstitial.setAdUnitId(Settings.INTERSTITIAL_ID);

		loadInterstitial();

		interstitial.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
			}

			@Override
			public void onAdClosed() {
				loadInterstitial();
				scheduleInterstitial();
			}
		});
		scheduleInterstitial();
	}

	private void scheduleInterstitial() {
		handler.postDelayed(new Runnable() {

			public void run() {
				if (interstitial.isLoaded())
					interstitial.show();
			}
		}, AD_LOAD_DELAY);
	}

	private void loadInterstitial() {
		AdRequest adRequest = new AdRequest.Builder().build();
		interstitial.loadAd(adRequest);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public void initializeScreenSize() {
		Display display = getWindowManager().getDefaultDisplay();

		if (android.os.Build.VERSION.SDK_INT >= 13) {
			Point size = new Point();
			display.getSize(size);
			screenWidth = size.x;
			screenHeight = size.y;
		} else {
			screenWidth = display.getWidth(); // deprecated
			screenHeight = display.getHeight(); // deprecated
		}
	}

	public void addDrum(View view) {
		addDrum(false);
		drumCount++;
	}

	public void newFile(View view) {
		drumCount = 6;
		removeAndAddTicks();
	}

	public void play(View view) {
		btnPlay.setEnabled(false);
		findViewById(R.id.btnNewFile).setEnabled(false);
		btnStop.setEnabled(true);
		playerThread = new Thread(new Player());
		playerThread.start();
	}

	public void stop(View view) {
		btnStop.setEnabled(false);
		findViewById(R.id.btnNewFile).setEnabled(true);
		btnPlay.setEnabled(true);
		if (playerThread != null)
			playerThread.interrupt();
	}

	public void clear(View view) {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					// Yes button clicked

					for (int i = 0; i < btnSequencerBarTicks.size(); i++) {
						for (int j = 0; j < btnSequencerBarTicks.get(i).size(); j++)
							btnSequencerBarTicks.get(i).get(j).setChecked(false);
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					// No button clicked
					break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(BoomJunkie.this);
		builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
	}

	@Override
	protected void onPause() {
		handler.removeCallbacksAndMessages(null);
		super.onPause();
	}

	public void initializeViews() {
		layoutDrums = (LinearLayout) findViewById(R.id.layoutDrums);
		btnPlay = (ImageButton) findViewById(R.id.btnPlay);

		Button btnAddDrum = (Button) findViewById(R.id.btnAddDrum);
		ImageButton btnNewFile = (ImageButton) findViewById(R.id.btnNewFile);
		buttonSideLength = (screenHeight - btnAddDrum.getLayoutParams().height - btnNewFile.getLayoutParams().height) / 7;

		seekLength = (SeekBar) findViewById(R.id.seekLength);
		seekBPM = (SeekBar) findViewById(R.id.seekBPM);

		textBPM = (TextView) findViewById(R.id.textBPM);
		textLength = (TextView) findViewById(R.id.textLength);
		seekLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

				textLength.setText("Length: " + (progress + 1) + " beats");
				stop(null);
				sequencerTickCount = (progress + 1) * 4;
				while (playerThread.isAlive()) {
					// Wait until thread ends.
					try {
						Thread.sleep(50);
					} catch (Exception e) {
						Log.e("Exception", e.getMessage());
					}
				}
				removeAndAddTicks();
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		seekBPM.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				bpm = progress + 30;
				textBPM.setText("BPM: " + bpm);
				tickInterval = 15000 / bpm;
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		layoutsSequencerTicks = new ArrayList<LinearLayout>();
		btnDrums = new ArrayList<DrumButton>();
		btnSequencerBarTicks = new ArrayList<ArrayList<SequencerTickButton>>();
		layoutSequencerTicks = (LinearLayout) findViewById(R.id.layoutSequencerTicks);

		hScroll = (HorizontalScrollView) findViewById(R.id.scrollViewTicksHorizontal);
		vScroll = (ScrollView) findViewById(R.id.scrollViewDrums);

		addDrum(true);
		for (int j = 0; j < drumCount; j++)
			addDrum(false);

		btnStop = (ImageButton) findViewById(R.id.btnStop);
		btnStop.setEnabled(false);

		checkLoop = (CheckBox) findViewById(R.id.checkLoop);
		checkLoop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				isLoop = isChecked;
			}
		});

		vScroll.setOnTouchListener(new View.OnTouchListener() { // inner scroll
																// listener

			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}
		});
		hScroll.setOnTouchListener(new View.OnTouchListener() { // outer scroll
																// listener
			private float mx, my, curX, curY;
			private boolean started = false;

			public boolean onTouch(View v, MotionEvent event) {
				curX = event.getX();
				curY = event.getY();
				int dx = (int) (mx - curX);
				int dy = (int) (my - curY);
				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE:
					if (started) {
						vScroll.scrollBy(0, dy);
						hScroll.scrollBy(dx, 0);
					} else {
						started = true;
					}
					mx = curX;
					my = curY;
					break;
				case MotionEvent.ACTION_UP:
					vScroll.scrollBy(0, dy);
					hScroll.scrollBy(dx, 0);
					started = false;
					break;
				}
				return false;
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	protected SoundPool createSoundPoolWithBuilder(int maxStreamCount) {
		AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

		return new SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(maxStreamCount).build();
	}

	public void initializeSounds() {
		// Initialize sounds
		Field[] fields = R.raw.class.getFields();

		// initiate soundpool according to Android API version
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			drumPlayer = createSoundPoolWithBuilder(fields.length - 1);
		else
			drumPlayer = new SoundPool(fields.length - 1, AudioManager.STREAM_MUSIC, 0);

		soundIDs = new int[fields.length - 1];

		for (int i = 0; i < fields.length - 1; i++) {
			try {
				soundIDs[i] = drumPlayer.load(this, fields[i].getInt(fields[i]), 1);
			} catch (Exception e) {// (IllegalAccessException e) {
				Log.e("IllegalAccessException", "Can't load: " + i);
			}
		}
	}

	class Player implements Runnable {
		public int j = 0;

		public void run() {
			boolean isPlayed = false;
			boolean isStopped = false;
			while (!isPlayed && !isStopped) {
				for (j = 0; j < btnSequencerBarTicks.get(0).size(); j++) {
					runOnUiThread(new Runnable() {

						public void run() {
							for (int ii = 0; ii < btnSequencerBarTicks.get(0).size(); ii++)
								btnSequencerBarTicks.get(0).get(ii).setChecked(false);

							btnSequencerBarTicks.get(0).get(j).setChecked(true);
						}
					});
					try {
						for (int i = 0; i < btnSequencerBarTicks.size(); i++) {
							if (btnSequencerBarTicks.get(i).get(j).isChecked() && btnDrums.get(i).isActive())
								drumPlayer.play(soundIDs[btnDrums.get(i).getDatabaseField()], 1, 1, 0, 0, 1);
						}

						Thread.sleep(tickInterval);
					} catch (InterruptedException e) {
						isStopped = true;
						break;
					}

				}
				if (isLoop)
					isPlayed = false;
				else
					isPlayed = true;
			}
			runOnUiThread(new Runnable() {

				public void run() {
					btnPlay.setEnabled(true);
					btnStop.setEnabled(false);
				}
			});
		}
	}

	public void showDrums() {
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
		// builderSingle.setIcon(R.drawable.btn_play);
		builderSingle.setTitle("Select a Drum");
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
		Field[] fields = R.raw.class.getFields();

		for (int i = 0; i < fields.length - 1; i++)
			if (!fields[i].getName().equals("gtm_analytics"))
				arrayAdapter.add(fields[i].getName().replace("d_", ""));

		builderSingle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				String strName = arrayAdapter.getItem(which);
				setDrumButtonNameAndIndex(strName, which);
			}
		});
		builderSingle.create();
		builderSingle.show();
	}

	public void setDrumButtonNameAndIndex(String name, int which) {
		btnDrums.get(currentClickedDrumButton).setText(name);
		btnDrums.get(currentClickedDrumButton).setDatabaseField(which);
		btnDrums.get(currentClickedDrumButton).setIsActive(true);
	}

	public void loadFromFile(View view) {
		File dir = getFilesDir();
		subFiles = dir.listFiles();

		AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
		// builderSingle.setIcon(R.drawable.btn_play);
		builderSingle.setTitle("Load Saved File");
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);

		if (subFiles != null) {
			for (File file : subFiles) {
				arrayAdapter.add(file.getName());
			}
		}

		builderSingle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				try {
					btnStop.setEnabled(false);
					btnPlay.setEnabled(true);
					if (playerThread != null)
						playerThread.interrupt();
					FileInputStream fis = openFileInput(subFiles[which].getName());
					HackedObjectInputStream is = new HackedObjectInputStream(fis);
					saveData = (SaveData) is.readObject();
					is.close();
					fis.close();
					interpretSaveData();

				} catch (Exception ioe) {
					Log.e("Error", "IOException", ioe);
				}
			}
		});
		builderSingle.create();
		builderSingle.show();

	}

	class HackedObjectInputStream extends ObjectInputStream {

		public HackedObjectInputStream(InputStream in) throws IOException {
			super(in);
		}

		@Override
		protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
			ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

			if (resultClassDescriptor.getName().equals("com.batsoft.boomjunkie.SaveData"))
				resultClassDescriptor = ObjectStreamClass.lookup(SaveData.class);

			return resultClassDescriptor;
		}
	}

	public void interpretSaveData() {
		this.bpm = saveData.bpm;
		seekBPM.setProgress(this.bpm);
		this.sequencerTickCount = saveData.sequencerTickCount;
		seekLength.setProgress(sequencerTickCount / 4 - 1);
		this.drumCount = saveData.drumCount;

		removeAndAddTicks();

		boolean[][] sequencerTicks = saveData.sequencerTicks;
		for (int i = 0; i < btnSequencerBarTicks.size(); i++) {
			for (int j = 0; j < btnSequencerBarTicks.get(0).size(); j++) {
				btnSequencerBarTicks.get(i).get(j).setChecked(sequencerTicks[i][j]);
			}
		}
		Field[] fields = R.raw.class.getFields();
		int[] btnDrumsIndex = saveData.btnDrumsIndex;
		int[] btnDrumsDatabaseField = saveData.btnDrumsDatabaseField;
		boolean[] btnDrumsIsActive = saveData.btnDrumsIsActive;
		for (int i = 0; i < btnDrums.size(); i++) {
			btnDrums.get(i).setIndex(btnDrumsIndex[i]);
			btnDrums.get(i).setDatabaseField(btnDrumsDatabaseField[i]);
			btnDrums.get(i).setIsActive(btnDrumsIsActive[i]);
			if (btnDrumsDatabaseField[i] == -1)
				btnDrums.get(i).setText("Empty");
			else if (btnDrumsDatabaseField[i] == -2)
				btnDrums.get(i).setText("Time");
			else
				btnDrums.get(i).setText(fields[btnDrumsDatabaseField[i]].getName().replace("d_", ""));
		}
	}

	public void saveToFile(View view) {
		boolean[][] sequencerTicks = new boolean[btnSequencerBarTicks.size()][btnSequencerBarTicks.get(0).size()];
		for (int i = 0; i < btnSequencerBarTicks.size(); i++) {
			for (int j = 0; j < btnSequencerBarTicks.get(0).size(); j++) {
				sequencerTicks[i][j] = btnSequencerBarTicks.get(i).get(j).isChecked();
			}
		}
		int[] btnDrumsIndex = new int[btnDrums.size()];
		int[] btnDrumsDatabaseField = new int[btnDrums.size()];
		boolean[] btnDrumsIsActive = new boolean[btnDrums.size()];
		for (int i = 0; i < btnDrums.size(); i++) {
			btnDrumsIndex[i] = btnDrums.get(i).getIndex();
			btnDrumsDatabaseField[i] = btnDrums.get(i).getDatabaseField();
			btnDrumsIsActive[i] = btnDrums.get(i).isActive();
		}
		saveData = new SaveData(sequencerTicks, btnDrumsIndex, btnDrumsDatabaseField, btnDrumsIsActive, bpm - 30, sequencerTickCount, drumCount);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Save to File");

		final EditText input = new EditText(this);

		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
		builder.setView(input);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				filename = input.getText().toString();
				try {

					FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
					ObjectOutputStream os = new ObjectOutputStream(fos);
					os.writeObject(saveData);
					os.close();
					fos.close();
				} catch (IOException e) {
					Log.e("Error", "FileReadError occurred.");
				}
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();

	}

	public void removeAndAddTicks() {
		for (int j = 0; j < btnDrums.size(); j++) {
			DrumButton btnDrum = btnDrums.get(j);
			layoutDrums.removeView(btnDrum);
			LinearLayout layout = layoutsSequencerTicks.get(j);

			layoutSequencerTicks.removeView(layout);

		}

		layoutsSequencerTicks = new ArrayList<LinearLayout>();
		btnDrums = new ArrayList<DrumButton>();
		btnSequencerBarTicks = new ArrayList<ArrayList<SequencerTickButton>>();
		addDrum(true);
		for (int i = 0; i < drumCount; i++)
			addDrum(false);

	}

	public void addDrum(boolean isTimeBar) {

		if (btnDrums.size() < maxDrumCount) {
			// Add the drum button on the left
			DrumButton btnDrum = new DrumButton(this, btnDrums.size());
			btnDrums.add(btnDrum);
			btnDrum.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);

			if (isTimeBar) {
				btnDrum.setText("Time");
				btnDrum.setBackgroundResource(R.drawable.btn_green);
				btnDrum.setTextColor(Color.parseColor("#FFFFFF"));
				btnDrum.setDatabaseField(-2);
				btnDrum.setClickable(false);
			} else {
				btnDrum.setText("Empty");
				btnDrum.setOnClickListener(new View.OnClickListener() {

					public void onClick(View view) {
						DrumButton drumButton = (DrumButton) view;
						currentClickedDrumButton = drumButton.getIndex();
						showDrums();
					}
				});
				btnDrum.setDatabaseField(-1);
			}

			layoutDrums.addView(btnDrum);

			LinearLayout.LayoutParams btnDrumLayoutParams = (LinearLayout.LayoutParams) btnDrum.getLayoutParams();
			btnDrumLayoutParams.height = buttonSideLength;

			LinearLayout layout = new LinearLayout(this);
			layoutsSequencerTicks.add(layout);
			layout.setOrientation(LinearLayout.HORIZONTAL);
			layout.setGravity(Gravity.LEFT);
			layoutSequencerTicks.addView(layout);

			btnSequencerBarTicks.add(new ArrayList<SequencerTickButton>());
			for (int i = 0; i < sequencerTickCount; i++) {
				SequencerTickButton btnTick = new SequencerTickButton(this);
				btnTick.setBlueOrangeBackground();
				btnSequencerBarTicks.get(btnSequencerBarTicks.size() - 1).add(btnTick);

				layout.addView(btnTick);
				btnTick.setText(null);
				btnTick.setTextOn(null);
				btnTick.setTextOff(null);

				btnTick.setIncludeFontPadding(true);
				if (isTimeBar) {
					btnTick.setText("" + (i + 1));
					btnTick.setEnabled(false);
					btnTick.setTextColor(Color.WHITE);
					btnTick.setGreenPurpleBackground();
				}
				btnTick.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				btnTick.setPadding(0, 0, 0, 0);

				LinearLayout.LayoutParams btnTickLayoutParams = (LinearLayout.LayoutParams) btnTick.getLayoutParams();
				btnTickLayoutParams.setMargins(0, 0, 0, 0);
				btnTickLayoutParams.width = buttonSideLength;
				btnTickLayoutParams.height = buttonSideLength;
			}
			vScroll.smoothScrollTo(0, vScroll.getBottom());
		} else {
			Toast maxSizeToast = Toast.makeText(this, "Max Drum Count Reached: " + maxDrumCount, Toast.LENGTH_SHORT);
			maxSizeToast.show();
		}
	}
}