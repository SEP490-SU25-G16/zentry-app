package vn.edu.fpt.zentryapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // No need to modify navigation - the default navigation in nav_graph_root.xml 
        // already starts with the login screen (loginFragment)
        
        // Log that we're using the default navigation
        android.util.Log.d("MainActivity", "Using default navigation starting with login screen");
    }
}