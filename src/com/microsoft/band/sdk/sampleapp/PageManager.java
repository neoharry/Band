package com.microsoft.band.sdk.sampleapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sdk.sampleapp.tileevent.R;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.pages.FlowPanel;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.HorizontalAlignment;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.ScrollFlowPanel;
import com.microsoft.band.tiles.pages.TextBlock;
import com.microsoft.band.tiles.pages.TextBlockFont;
import com.microsoft.band.tiles.pages.TextButton;
import com.microsoft.band.tiles.pages.TextButtonData;
import com.microsoft.band.tiles.pages.VerticalAlignment;
import com.microsoft.band.tiles.pages.WrappedTextBlock;
import com.microsoft.band.tiles.pages.WrappedTextBlockData;
import com.microsoft.band.tiles.pages.WrappedTextBlockFont;

import java.util.List;
import java.util.UUID;

/**
 * Created by ssebast on 7/27/2016.
 */
public class PageManager{
    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    private static final UUID homePageId = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd03");
    private static final UUID questionPageId = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd00");

    private static int health = 2;

    private BandClient client = null;
    private Question q;

    static int currentIndex = 0;

    Context context;
    public PageManager(Context context)
    {
        this.context = context;
    }

    public void createHomeView() {
        try {
            client.getTileManager().removePages(tileId);
            client.getTileManager().setPages(tileId,
                    new PageData(homePageId, 0)
                            .update(new WrappedTextBlockData(1, "Lives: " + health))
                            .update(new TextButtonData(99, "Play")));
        } catch (BandIOException e) {
            e.printStackTrace();
        }
    }

    public void removeTile() throws BandIOException, InterruptedException, BandException {
        if (doesTileExist()) {
            client.getTileManager().removeTile(tileId).await();
        }
    }


    private boolean doesTileExist() throws BandIOException, InterruptedException, BandException {
        List<BandTile> tiles = client.getTileManager().getTiles().await();
        for (BandTile tile : tiles) {
            if (tile.getTileId().equals(tileId)) {
                return true;
            }
        }
        return false;
    }

    public boolean addTile(BandTileEventAppActivity activity) throws Exception {
        if (doesTileExist()) {
            return true;
        }

		/* Set the options */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap tileIcon = BitmapFactory.decodeResource(context.getResources(), R.raw.b_icon, options);

        BandTile tile = new BandTile.Builder(tileId, "Button Tile", tileIcon)
                .setPageLayouts(createOneButtonLayout(), createButtonLayout())
                .build();
        //appendToUI("Button Tile is adding ...\n");
        if (client.getTileManager().addTile(activity, tile).await()) {
            //appendToUI("Button Tile is added.\n");
            return true;
        } else {
            //appendToUI("Unable to add button tile to the band.\n");
            return false;
        }
    }

    public void createQuestion()
    {
        Question.generateQuestions(context);
        try {
            if (currentIndex >= 3 || currentIndex < 0)
            {
                //Purge all pages if any
                createHomeView();
                return ;
            }

            updatePages();
        }
        catch(BandIOException e)
        {}
        catch(BandException e)
        {}
    }

    private PageLayout createOneButtonLayout()
    {
        TextButton button1 = new TextButton(0, 5, 100, 45).setMargins(0, 5, 0, 0).setId(99).setPressedColor(Color.YELLOW);

        return new PageLayout(
                new ScrollFlowPanel(0, 0, 245, 100, FlowPanelOrientation.VERTICAL).setHorizontalAlignment(HorizontalAlignment.CENTER).setVerticalAlignment(VerticalAlignment.TOP)
                        .addElements(
                                new WrappedTextBlock(0, 0, 245, 202, WrappedTextBlockFont.SMALL).setId(1).setColor(Color.CYAN).setAutoHeightEnabled(true),
                                new TextBlock(0,0,245, 50, TextBlockFont.SMALL).setId(2).setColor(Color.YELLOW).setAutoWidthEnabled(true),
                                new FlowPanel(0,0,100,45,FlowPanelOrientation.HORIZONTAL).addElements(button1).setHorizontalAlignment(HorizontalAlignment.CENTER)));
    }

    private PageLayout createButtonLayout() {
        TextButton button1 = new TextButton(0, 5, 100, 45).setMargins(0, 5, 0, 0).setId(12).setPressedColor(Color.GRAY);
        TextButton button2 = new TextButton(0, 0, 100, 45).setMargins(5, 5, 0, 0).setId(21).setPressedColor(Color.GRAY);
        return new PageLayout(
                new ScrollFlowPanel(0, 0, 245, 100, FlowPanelOrientation.VERTICAL).setHorizontalAlignment(HorizontalAlignment.LEFT).setVerticalAlignment(VerticalAlignment.TOP)
                        .addElements(
                                new WrappedTextBlock(0, 0, 245, 202, WrappedTextBlockFont.SMALL).setId(1).setColor(Color.WHITE).setAutoHeightEnabled(true),
                                new FlowPanel(0,0,220,100,FlowPanelOrientation.HORIZONTAL).addElements(button1).addElements(button2)));
    }

    public void updatePages() throws BandIOException {
        q = Question.getAQuestion();
        String[] options = q.getOptions();
        client.getTileManager().setPages(tileId,
                new PageData(questionPageId, 1)
                        .update(new WrappedTextBlockData(1, q.getQuestionTitle()))
                        .update(new TextButtonData(12, options[0]))
                        .update(new TextButtonData(21, options[1])));
    }

    public void OnAnswered(UUID pageId, Boolean isCorrect) throws BandException, InterruptedException {
        String message = (isCorrect?"Correct!!":"Wrong :(");
        message += "\n" + q.getMessage();
        String[] options = q.getOptions();

        client.getTileManager().setPages(tileId,
                new PageData(pageId, 1)
                        .update(new WrappedTextBlockData(1, message))
                        .update(new TextButtonData(12, options[0]))
                        .update(new TextButtonData(21, options[1])));

        if (isCorrect)
        {
            health++;
            if (health > 2) health = 2;
        }
        else
        {
            health--;
            if (health < 1) return ;
        }
        currentIndex++;

        Thread.sleep(3000);
        createQuestion();
    }

    public boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                //appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(context, devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        //appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

}
