package com.agentcore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.shared.ConnectionMode

@Composable
fun SetupInstructions(
    mode: ConnectionMode,
    onBack: () -> Unit,
    onReady: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Setup: ${mode.name} Mode",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            InstructionCard(mode)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onReady,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("Backend is ready, Connect!", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun InstructionCard(mode: ConnectionMode) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            when (mode) {
                ConnectionMode.IPC -> IpcInstructions()
                ConnectionMode.UNIX_SOCKET -> UnixSocketInstructions()
                ConnectionMode.STDIO -> StdioInstructions()
                ConnectionMode.CLI -> CliInstructions()
            }
        }
    }
}

@Composable
fun IpcInstructions() {
    Text("1. Open your terminal.", color = Color.LightGray)
    Spacer(modifier = Modifier.height(8.dp))
    Text("2. Run the Agent Core server:", color = Color.LightGray)
    CodeBlock("agent-core server --port 7700")
    Spacer(modifier = Modifier.height(16.dp))
    Text("3. The application will connect to:", color = Color.LightGray)
    Text("http://localhost:7700/v1/sse", color = Color(0xFF81D4FA), fontFamily = FontFamily.Monospace)
}

@Composable
fun UnixSocketInstructions() {
    Text("1. Open your terminal.", color = Color.LightGray)
    Spacer(modifier = Modifier.height(8.dp))
    Text("2. Run the server with a socket path:", color = Color.LightGray)
    CodeBlock("agent-core server --socket /tmp/agent.sock")
    Spacer(modifier = Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Ensure the socket file exists before connecting.", color = Color.Yellow, fontSize = 12.sp)
    }
}

@Composable
fun StdioInstructions() {
    Text("1. Ensure 'agent-core' binary is in your PATH.", color = Color.LightGray)
    Spacer(modifier = Modifier.height(8.dp))
    Text("2. Or set the specific path in Settings later.", color = Color.LightGray)
    Spacer(modifier = Modifier.height(16.dp))
    Text("The app will launch the process and communicate via standard input/output streams.", color = Color.Gray, fontSize = 14.sp)
}

@Composable
fun CliInstructions() {
    Text("1. This mode executes 'agent-core' directly for each prompt.", color = Color.LightGray)
    Spacer(modifier = Modifier.height(8.dp))
    Text("2. Make sure the binary is installed and works from terminal:", color = Color.LightGray)
    CodeBlock("agent-core --version")
    Spacer(modifier = Modifier.height(16.dp))
    Text("Ideal for quick, one-shot tasks without a background server.", color = Color.Gray, fontSize = 14.sp)
}

@Composable
fun CodeBlock(code: String) {
    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = code,
                color = Color(0xFF00E676),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* TODO: Copy to clipboard */ }) {
                Icon(Icons.Default.Info, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}
