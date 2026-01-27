package org.github.srtobi.tradewar3.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.graphics.{Color, Pixmap, Texture}
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.{Actor, Stage}
import com.badlogic.gdx.utils.viewport.ScreenViewport
import org.github.srtobi.tradewar3.Tradewar3
import org.github.srtobi.tradewar3.net.*
import org.github.srtobi.tradewar3.ui.StarfieldBackground
import org.github.srtobi.tradewar3.GameConfig.*

import scala.compiletime.uninitialized

class MainMenuScreen(game: Tradewar3) extends BaseScreen:
  private val stage = new Stage(new ScreenViewport())
  private val starfield = new StarfieldBackground(500) // More stars for menu
  private var skin: Skin = uninitialized

  override def show(): Unit =
    Gdx.input.setInputProcessor(stage)
    skin = createSimpleSkin()

    val table = new Table()
    table.setFillParent(true)
    stage.addActor(table)

    // Title with glow effect
    val titleLabel = new Label("TRADEWAR", skin, "title")
    titleLabel.setColor(new Color(0.6f, 0.8f, 1f, 1f))
    val subtitleLabel = new Label("GALAXY", skin, "subtitle")
    subtitleLabel.setColor(new Color(1f, 0.85f, 0.5f, 1f))

    val nameField = new TextField("Commander", skin)
    val hostButton = new TextButton("HOST GAME", skin)

    val ipField = new TextField("localhost", skin)
    val joinButton = new TextButton("JOIN GAME", skin)

    val quitButton = new TextButton("QUIT", skin)

    hostButton.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        val server = new GameServer(DEFAULT_PORT)
        val client = new GameClient("localhost", DEFAULT_PORT)
        client.send(JoinRequest(nameField.getText))
        game.setScreen(new LobbyScreen(game, Some(server), client))
    })

    joinButton.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        val client = new GameClient(ipField.getText, DEFAULT_PORT)
        client.send(JoinRequest(nameField.getText))
        game.setScreen(new LobbyScreen(game, None, client))
    })

    quitButton.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        Gdx.app.exit()
    })

    // Layout with visual containers
    table.add(titleLabel).padBottom(5)
    table.row()
    table.add(subtitleLabel).padBottom(60)
    table.row()

    // Name section
    val nameLabel = new Label("COMMANDER NAME", skin)
    nameLabel.setColor(new Color(0.7f, 0.75f, 0.85f, 1f))
    table.add(nameLabel).pad(5)
    table.row()
    table.add(nameField).pad(10).width(320).height(55)
    table.row()
    table.add(hostButton).pad(15).width(320).height(70)
    table.row()

    // Join section
    val joinLabel = new Label("SERVER ADDRESS", skin)
    joinLabel.setColor(new Color(0.7f, 0.75f, 0.85f, 1f))
    table.add(joinLabel).padTop(30).pad(5)
    table.row()
    table.add(ipField).pad(10).width(320).height(55)
    table.row()
    table.add(joinButton).pad(15).width(320).height(70)
    table.row()
    table.add(quitButton).padTop(40).pad(10).width(220).height(55)

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

    // Default font with shadow
    val parameter = new FreeTypeFontParameter()
    parameter.size = 24
    parameter.shadowOffsetX = 1
    parameter.shadowOffsetY = 1
    parameter.shadowColor = new Color(0, 0, 0, 0.5f)
    val font = generator.generateFont(parameter)
    skin.add("default", font)

    // Large title font with glow
    val titleParameter = new FreeTypeFontParameter()
    titleParameter.size = 72
    titleParameter.shadowOffsetX = 3
    titleParameter.shadowOffsetY = 3
    titleParameter.shadowColor = new Color(0.2f, 0.3f, 0.5f, 0.8f)
    titleParameter.borderWidth = 2
    titleParameter.borderColor = new Color(0.4f, 0.6f, 0.9f, 0.5f)
    val titleFont = generator.generateFont(titleParameter)
    skin.add("title", titleFont)

    // Subtitle font
    val subtitleParameter = new FreeTypeFontParameter()
    subtitleParameter.size = 42
    subtitleParameter.shadowOffsetX = 2
    subtitleParameter.shadowOffsetY = 2
    subtitleParameter.shadowColor = new Color(0.3f, 0.2f, 0, 0.6f)
    val subtitleFont = generator.generateFont(subtitleParameter)
    skin.add("subtitle", subtitleFont)

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

    val subtitleLabelStyle = new Label.LabelStyle()
    subtitleLabelStyle.font = subtitleFont
    skin.add("subtitle", subtitleLabelStyle)

    // Enhanced button style
    val textButtonStyle = new TextButton.TextButtonStyle()
    textButtonStyle.up = skin.newDrawable("white", new Color(0.2f, 0.25f, 0.4f, 0.9f))
    textButtonStyle.down = skin.newDrawable("white", new Color(0.35f, 0.4f, 0.55f, 1f))
    textButtonStyle.over = skin.newDrawable("white", new Color(0.3f, 0.35f, 0.5f, 0.95f))
    textButtonStyle.font = skin.getFont("default")
    textButtonStyle.fontColor = new Color(0.9f, 0.95f, 1f, 1f)
    skin.add("default", textButtonStyle)

    // Enhanced text field style
    val textFieldStyle = new TextField.TextFieldStyle()
    textFieldStyle.font = font
    textFieldStyle.fontColor = new Color(0.9f, 0.95f, 1f, 1f)
    textFieldStyle.cursor = skin.newDrawable("white", new Color(0.7f, 0.85f, 1f, 1f))
    textFieldStyle.selection = skin.newDrawable("white", new Color(0.4f, 0.5f, 0.7f, 0.7f))
    textFieldStyle.background = skin.newDrawable("white", new Color(0.12f, 0.15f, 0.22f, 0.9f))
    skin.add("default", textFieldStyle)

    skin
