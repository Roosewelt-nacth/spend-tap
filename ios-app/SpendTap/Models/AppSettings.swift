import Foundation

struct AppSettings: Codable, Equatable {
    var monthlyIncome: Double = 0
    var monthlyBudget: Double = 0       // spending limit; 0 = derive from income minus savings
    var sheetsURL: String = ""          // Apps Script web-app URL of the user's own sheet
    var sheetsToken: String = ""
    var lastAutoExportedYear: Int? = nil

    init() {}

    // decodeIfPresent on every key: when a future version adds a field, a
    // settings.json written by an older version still loads instead of being thrown away.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        monthlyIncome = try c.decodeIfPresent(Double.self, forKey: .monthlyIncome) ?? 0
        monthlyBudget = try c.decodeIfPresent(Double.self, forKey: .monthlyBudget) ?? 0
        sheetsURL = try c.decodeIfPresent(String.self, forKey: .sheetsURL) ?? ""
        sheetsToken = try c.decodeIfPresent(String.self, forKey: .sheetsToken) ?? ""
        lastAutoExportedYear = try c.decodeIfPresent(Int.self, forKey: .lastAutoExportedYear)
    }
}
