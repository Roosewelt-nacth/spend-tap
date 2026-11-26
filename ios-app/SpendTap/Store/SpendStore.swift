import Foundation

/// Local JSON store. The App Intent lives in the app target and runs in the
/// app's own process, so no App Group is needed yet. Move to a shared App Group
/// container when the widget/Live Activity extensions are added.
final class SpendStore: @unchecked Sendable {
    static let shared = SpendStore()

    private let lock = NSLock()
    private let url: URL

    private init() {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        url = dir.appendingPathComponent("entries.json")
    }

    func all() -> [SpendEntry] {
        lock.lock(); defer { lock.unlock() }
        return load().sorted { $0.createdAt > $1.createdAt }
    }

    func add(_ entry: SpendEntry) {
        lock.lock(); defer { lock.unlock() }
        var entries = load()
        entries.append(entry)
        save(entries)
    }

    func delete(id: UUID) {
        lock.lock(); defer { lock.unlock() }
        save(load().filter { $0.id != id })
    }

    func todayTotal() -> Double {
        let start = Calendar.current.startOfDay(for: Date())
        return all().filter { $0.createdAt >= start }.reduce(0) { $0 + $1.amount }
    }

    // MARK: - private (callers hold the lock)

    private func load() -> [SpendEntry] {
        guard let data = try? Data(contentsOf: url) else { return [] }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return (try? decoder.decode([SpendEntry].self, from: data)) ?? []
    }

    private func save(_ entries: [SpendEntry]) {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        if let data = try? encoder.encode(entries) {
            try? data.write(to: url, options: .atomic)
        }
    }
}
