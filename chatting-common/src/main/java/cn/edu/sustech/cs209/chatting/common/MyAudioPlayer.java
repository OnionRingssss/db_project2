package cn.edu.sustech.cs209.chatting.common;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class MyAudioPlayer {
//    private URL url = "null"; // 音乐文件的URl
//    private AudioStream audioStream = null; // 播放器
//    public MyAudioPlayer() {
//        try {
//            File file = ResourceUtils.getFile("classpath:templates/WX.wav");
//            url = file.toURI().toURL();
//            InputStream inputStream = url.openStream(); // 获得音乐文件的输入流
//            audioStream = new AudioStream(inputStream);
//            System.out.println(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 用AudioPlayer静态成员player.start播放音乐
//     */
//    public void play() {
//        AudioPlayer.player.start(audioStream);
//    }
public static void playMusic() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    File wavFile = new File("C:\\Users\\y1211\\Desktop\\java2_assignment\\CS029A_assignment2\\6tn8s-wo9nn.wav");//可以使用文件
//InputStream in = new FileInputStream(wavFile);//也可以使用流
//URL url = wavFile.toURI().toURL();//还可以使用URL
    AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile);//这里使用上面的三种，那种都可以
    Clip clip = AudioSystem.getClip();
    clip.open(ais);
    clip.start();
}
}
