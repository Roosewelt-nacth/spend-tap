import Foundation

/// Mirrors the Supabase `entries` row (see desktop-app/schema.sql) so a later
/// sync step can upload these as-is.
struct SpendEntry: Codable, Identifiable, Equatable {
    var id: UUID = UUID()
    var amount: Double
    var category: String          // SpendCategory.rawValue
    var note: String = ""
    var paymentMethod: String = "upi"   // "upi" | "cash"
    var isImpulse: Bool = false
    var createdAt: Date = Date()
}
