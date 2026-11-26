import Foundation

/// Sends the yearly report to the user's own Google Sheet.
///
/// How it works without Google sign-in: the user pastes google-sheets/Code.gs into
/// Extensions > Apps Script on THEIR sheet and deploys it as a web app. That gives a
/// private URL. The app POSTs the report there and the script writes a tab named
/// after the year. The user chooses the sheet simply by whose script they deploy.
enum SheetsExporter {
    enum ExportError: LocalizedError {
        case badURL
        case server(String)

        var errorDescription: String? {
            switch self {
            case .badURL: "The sheet link must be an https:// Apps Script web-app URL."
            case .server(let m): m
            }
        }
    }

    private struct Payload: Encodable {
        var token: String
        var report: YearlyReport
    }

    private struct Reply: Decodable {
        var ok: Bool
        var error: String?
    }

    static func send(_ report: YearlyReport, urlString: String, token: String) async throws {
        guard let url = URL(string: urlString.trimmingCharacters(in: .whitespacesAndNewlines)),
              url.scheme == "https" else { throw ExportError.badURL }

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try encoder.encode(Payload(token: token, report: report))
        request.timeoutInterval = 30

        // Apps Script answers a POST with a redirect; URLSession follows it and
        // returns the JSON the script produced.
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw ExportError.server("Google returned an error. Check that the web app is deployed for \"Anyone\".")
        }
        guard let reply = try? JSONDecoder().decode(Reply.self, from: data) else {
            throw ExportError.server("Unexpected reply. Re-deploy the script and paste the new /exec URL.")
        }
        if !reply.ok { throw ExportError.server(reply.error ?? "The script rejected the request.") }
    }
}
