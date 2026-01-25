package org.github.srtobi.tradewar3.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.{Color, Pixmap, Texture}
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.scenes.scene2d.{Actor, Stage}
import com.badlogic.gdx.scenes.scene2d.ui.{Label, Skin, Table, TextButton}
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import org.github.srtobi.tradewar3.Tradewar3
import org.github.srtobi.tradewar3.ui.StarfieldBackground

import scala.compiletime.uninitialized

class MainMenuScreen(game: Tradewar3) extends BaseScreen:
  private val stage = new Stage(new ScreenViewport())
  private val starfield = new StarfieldBackground()
  private var skin: Skin = uninitialized

  override def show(): Unit =
    Gdx.input.setInputProcessor(stage)
    skin = createSimpleSkin()
    
    val table = new Table()
    table.setFillParent(true)
    stage.addActor(table)
    
    val titleLabel = new Label("TradeWar Galaxy", skin, "title")
    val startButton = new TextButton("Start New Game", skin)
    val quitButton = new TextButton("Quit", skin)
    
    startButton.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        game.setScreen(new GameScreen(game))
    })
    
    quitButton.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        Gdx.app.exit()
    })
    
    table.add(titleLabel).padBottom(50)
    table.row()
    table.add(startButton).pad(10).width(300).height(70)
    table.row()
    table.add(quitButton).pad(10).width(300).height(70)

  override def render(delta: Float): Unit =
    clearScreen()
    
    starfield.render(delta)
    
    stage.act(delta)
    stage.draw()

  override def resize(width: Int, height: Int): Unit =
    stage.getViewport.update(width, height, true)
    starfield.resize(width, height)

  override def dispose(): Unit =
    stage.dispose()
    starfield.dispose()
    if skin != null then skin.dispose()

  private def createSimpleSkin(): Skin =
    val skin = new Skin()
    val generator = new FreeTypeFontGenerator(Gdx.files.internal("assets/fonts/Roboto-Regular.ttf"))
    
    val parameter = new FreeTypeFontParameter()
    parameter.size = 24
    val font = generator.generateFont(parameter)
    skin.add("default", font)

    val titleParameter = new FreeTypeFontParameter()
    titleParameter.size = 48
    val titleFont = generator.generateFont(titleParameter)
    skin.add("title", titleFont)
    
    generator.dispose()
    
    val pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(Color.WHITE)
    pixmap.fill()
    val texture = new Texture(pixmap)
    pixmap.dispose()
    skin.add("white", texture)

    val labelStyle = new Label.LabelStyle()
    labelStyle.font = font
    skin.add("default", labelStyle)

    val titleLabelStyle = new Label.LabelStyle()
    titleLabelStyle.font = titleFont
    skin.add("title", titleLabelStyle)
    
    val textButtonStyle = new TextButton.TextButtonStyle()
    textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY)
    textButtonStyle.down = skin.newDrawable("white", Color.LIGHT_GRAY)
    textButtonStyle.over = skin.newDrawable("white", Color.GRAY)
    textButtonStyle.font = skin.getFont("default")
    skin.add("default", textButtonStyle)
    skin
