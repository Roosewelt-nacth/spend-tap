import Foundation

/// Tiny typed wrapper around one JSON file in Application Support.
struct JSONFile<T: Codable> {
    let url: URL
    let defaultValue: T

    init(name: String, defaultValue: T) {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        url = dir.appendingPathComponent(name)
        self.defaultValue = defaultValue
    }

    func load() -> T {
        guard let data = try? Data(contentsOf: url) else { return defaultValue }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return (try? decoder.decode(T.self, from: data)) ?? defaultValue
    }

    func save(_ value: T) {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        if let data = try? encoder.encode(value) {
            try? data.write(to: url, options: .atomic)
        }
    }
}
