import Foundation
import Observation

/// 侦查历史（本地 JSON 持久化，仅存设备）
@Observable
final class HistoryStore {
    private(set) var records: [ScanRecord] = []
    private let fileURL: URL

    init() {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        fileURL = dir.appending(path: "history.json")
        load()
    }

    func add(_ record: ScanRecord) {
        // 同值去重：保留最新
        records.removeAll { $0.type == record.type && $0.value == record.value }
        records.insert(record, at: 0)
        if records.count > 200 { records = Array(records.prefix(200)) }
        persist()
    }

    func clear() {
        records = []
        persist()
    }

    private func load() {
        guard let data = try? Data(contentsOf: fileURL),
              let list = try? JSONDecoder().decode([ScanRecord].self, from: data)
        else { return }
        records = list
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(records) else { return }
        try? data.write(to: fileURL, options: .atomic)
    }
}
