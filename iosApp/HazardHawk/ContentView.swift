import SwiftUI
import shared

struct ContentView: View {
    @State private var platformTest = PlatformTest()
    @State private var platformName = "Loading..."
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "hammer.fill")
                .font(.system(size: 80))
                .foregroundColor(.orange)
            
            Text("HazardHawk")
                .font(.largeTitle)
                .fontWeight(.bold)
            
            Text("Construction Safety AI Platform")
                .font(.headline)
                .foregroundColor(.secondary)
            
            VStack(alignment: .leading, spacing: 10) {
                Text("Platform Information:")
                    .font(.headline)
                    .padding(.bottom, 5)
                
                Text("Device: \(platformName)")
                    .font(.body)
                
                Text("Status: iOS App Running")
                    .font(.body)
                    .foregroundColor(.green)
                
                Text("KMP Integration: Active")
                    .font(.body)
                    .foregroundColor(.blue)
            }
            .padding()
            .background(Color.gray.opacity(0.1))
            .cornerRadius(10)
            
            Button("Test Platform") {
                platformName = platformTest.getPlatformName()
            }
            .padding()
            .background(Color.orange)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
        .padding()
        .onAppear {
            platformName = platformTest.getPlatformName()
        }
    }
}

#Preview {
    ContentView()
}