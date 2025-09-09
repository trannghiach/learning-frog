package io.github.trannghiach.learning_frog;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main implements ApplicationListener {
    public static final int VIRTUAL_W = 320, VIRTUAL_H = 180;
    public static final int TILE = 16;
    public static final int PLAYER_W = 16, PLAYER_H = 16;

    private SpriteBatch batch;
    private OrthographicCamera cam;
    private Viewport viewport;

    private Texture sheet;
    private TextureRegion[][] frames;
    private Animation<TextureRegion> jumpDown, jumpLeft, jumpRight, jumpUp;
    private TextureRegion current;
    private float animTime = 0f;
    private boolean moving = false;
    private int facing = 0;

    private float px = VIRTUAL_W/2f - 8, py = VIRTUAL_H/2f - 8;

    private BitmapFont font;
    private boolean showDebug;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private final Array<Rectangle> colliders = new Array<>();
    private int mapWpx, mapHpx;

    @Override
    public void create() {
        // Prepare your application here.
        batch = new SpriteBatch();
        cam = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_W, VIRTUAL_H, cam);
        viewport.apply(true);

        map = new TmxMapLoader().load("maps/map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);

        int mapWtiles = map.getProperties().get("width", Integer.class);
        int mapHtiles = map.getProperties().get("height", Integer.class);
        int tileW = map.getProperties().get("tilewidth", Integer.class);
        int tileH = map.getProperties().get("tileheight", Integer.class);
        mapWpx = mapWtiles * tileW;
        mapHpx = mapHtiles * tileH;

        cam.position.set(VIRTUAL_W/2f, VIRTUAL_H/2f, 0);
        cam.update();

        sheet = new Texture(Gdx.files.internal("sprites/frog-sprite-sheet.png"));
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        frames = TextureRegion.split(sheet, TILE, TILE);

        jumpDown = new Animation<>(0.12f, frames[0]);
        jumpLeft = new Animation<>(0.12f, frames[1]);
        jumpRight = new Animation<>(0.12f, frames[2]);
        jumpUp = new Animation<>(0.12f, frames[3]);

        font = new BitmapFont();

        MapLayer colLayer = map.getLayers().get("collision");
        if(colLayer != null) {
            for(MapObject obj : colLayer.getObjects()){
                if(obj instanceof RectangleMapObject) {
                    colliders.add(((RectangleMapObject) obj).getRectangle());
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;
        viewport.update(width, height, true);
        // Resize your application here. The parameters represent the new window size.
    }


    @Override
    public void render() {
        // Draw your application here.

        float dt = Gdx.graphics.getDeltaTime();
        moving = false;
        float spd = 80f;

        float nx = px, ny = py;

        if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            nx -= spd * dt;
            current = jumpLeft.getKeyFrame(animTime, true);
            moving = true;
            facing = 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            nx += spd * dt;
            current = jumpRight.getKeyFrame(animTime, true);
            moving = true;
            facing = 2;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.W)) {
            ny += spd * dt;
            current = jumpUp.getKeyFrame(animTime, true);
            moving = true;
            facing = 3;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S)) {
            ny -= spd * dt;
            current = jumpDown.getKeyFrame(animTime, true);
            moving = true;
            facing = 0;
        }

        if(moving) animTime += dt; else {
            switch (facing) {
                case 1: current = frames[1][0]; break;
                case 2: current = frames[2][0]; break;
                case 3: current = frames[3][0]; break;
                default: current = frames[0][0]; break;
            }
        }

        Rectangle test = new Rectangle(nx, py, PLAYER_W, PLAYER_H);
        if(!hitsCollision(test)) px = nx;

        test.set(px, ny, PLAYER_W, PLAYER_H);
        if(!hitsCollision(test)) py = ny;

        px = MathUtils.clamp(px, 0, mapWpx - PLAYER_W);
        py = MathUtils.clamp(py, 0, mapHpx - PLAYER_H);

        float halfW = VIRTUAL_W / 2f;
        float halfH = VIRTUAL_H / 2f;
        float camX =MathUtils.clamp(px + PLAYER_W / 2f, halfW, Math.max(halfW, mapWpx - halfW));
        float camY =MathUtils.clamp(py + PLAYER_H / 2f, halfH, Math.max(halfH, mapHpx - halfH));
        cam.position.set(Math.round(camX), Math.round(camY), 0);
        cam.update();

        if(Gdx.input.isKeyJustPressed(Input.Keys.F3)) showDebug = !showDebug;

        Gdx.gl.glClearColor(0.12f, 0.14f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mapRenderer.setView(cam);
        mapRenderer.render();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        batch.draw(current, Math.round(px), Math.round(py), PLAYER_W, PLAYER_H);
        if(showDebug) {
            font.draw(batch, "x = " + (int) px + " y = " + (int) py + " fps = " + Gdx.graphics.getFramesPerSecond(), 4, VIRTUAL_H - 4);
        }
        batch.end();
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    private boolean hitsCollision(Rectangle r) {
        for(Rectangle c: colliders) {
            if(c.overlaps(r)) return true;
        }
        return false;
    }

    @Override
    public void dispose() {
        // Destroy application's resources here.
        batch.dispose();
        sheet.dispose();
        font.dispose();
        mapRenderer.dispose();
        map.dispose();
    }
}
